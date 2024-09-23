@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp.tts

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.demo.pet.petapp.Constants
import com.demo.pet.petapp.PetApplication
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import java.nio.ByteBuffer

class TTSControllerGoogleCloudApi(var context: Context, val option: String) : TTSController {
    override var callback: TTSController.Callback? = null
    override var isSpeaking: Boolean = false

    private val refreshTask: RefreshAccessTokenTask

    private val LANG = "ja-JP"

    private data class Voice(
            val name: String,
            val ssmlGender: String,
            val naturalSampleRateHertz: Int)
    private val voices: MutableList<Voice> = mutableListOf()
    private var selectedVoice: Voice? = null // null means default

    private var player: AudioTrack? = null
    private var playerBufSize: Int = 0
    private var frameSizeInBytes: Int = 0

    companion object {
        private const val IS_DEBUG = Log.IS_DEBUG || false

        private val backThread = HandlerThread("gcp-tts", Thread.NORM_PRIORITY)
        private val backHandler: Handler

        private val authTask = RequestOAuth2AuthorizationCodeTask()
        private val requestTokenTask = RequestTokenTask()

        private val clientId: String
        private var authorizationCode: String
        private var refreshToken: String
        private var accessToken: String

        private external fun getId(): String

        init {
            System.loadLibrary("config")
            clientId = getId()
            authorizationCode = ""
            refreshToken = ""
            accessToken = ""

            backThread.start()
            backHandler = Handler(backThread.looper)

        }

        private lateinit var getCodeActResLauncher: ActivityResultLauncher<Intent>

        fun onStaticCreate(activity: AppCompatActivity) {
            if (IS_DEBUG) debugLog("onStaticCreate() : E")

            getCodeActResLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (IS_DEBUG) {
                    debugLog("getCodeActResLauncher.onActivityResult()")
                    debugLog("## result = $result")
                    debugLog("## result.data = ${result.data}")
                }
            }

            if (IS_DEBUG) debugLog("onStaticCreate() : X")
        }

        fun onStaticResume(code: String) {
            if (IS_DEBUG) debugLog("onStaticResume() : E")

            authorizationCode = code;
            backHandler.post(requestTokenTask)

            if (IS_DEBUG) debugLog("onStaticResume() : X")
        }

        private class RequestTokenTask : Runnable {
            private val TOKEN_URL = "https://oauth2.googleapis.com/token"

            override fun run() {
                if (IS_DEBUG) debugLog("RequestTokenTask.run() : E")

                val body = "client_id=${clientId}&code=${authorizationCode}&code_verifier=${Constants.GCP_CODE_VERIFIER}&grant_type=authorization_code&redirect_uri=${Constants.GCP_REDIRECT_URI}"
                if (IS_DEBUG) debugLog("RequestTokenTask.run() : body = $body")

                val client = OkHttpClient()
                val requestBody = body.toRequestBody(Constants.GCP_CONTENT_TYPE)
                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    if (IS_DEBUG) debugLog("RequestTokenTask.run() : Get access-token OK")

                    response.body.let {
                        val responseBody = it.string()

                        if (IS_DEBUG) {
                            debugLog("## RESPONSE")
                            debugLog(responseBody)
                        }

                        val mapper = jacksonObjectMapper()
                        val rootNode = mapper.readTree(responseBody)

                        accessToken = rootNode.get("access_token").asText()
                        refreshToken = rootNode.get("refresh_token").asText()

                        if (IS_DEBUG) {
                            debugLog("## rootNode = $rootNode")
                            debugLog("## refreshToken = $refreshToken")
                            debugLog("## accessToken = $accessToken")
                        }

                        PetApplication.getSP().edit().putString(Constants.KEY_GCP_REFRESH_TOKEN, refreshToken).apply()

                    }
                } else {
                    errorLog("RequestTokeTask.run() : Get access-token NG")
                    errorLog("## NG Response = $response")
                    errorLog("## NG Response body = ${response.body}")
                    errorLog("## NG Response headers = ${response.headers}")
                    errorLog("## NG Response message = ${response.message}")
                }

                if (IS_DEBUG) debugLog("RequestTokenTask.run() : X")
            }
        }


        fun updateRefreshToken() {
            if (IS_DEBUG) debugLog("TTSController.GCP.updateRefreshToken() : E")

            backHandler.post(authTask)

            if (IS_DEBUG) debugLog("TTSController.GCP.updateRefreshToken() : X")
        }

        private class RequestOAuth2AuthorizationCodeTask : Runnable {
            override fun run() {
                if (IS_DEBUG) debugLog("RequestOAuth2AuthorizationCodeTask.run() : E")

                val url = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=${clientId}&redirect_uri=${Constants.GCP_REDIRECT_URI}&scope=${Constants.GCP_SCOPE}&code_challenge=${Constants.GCP_CODE_VERIFIER}&code_challenge_method=${Constants.GCP_CODE_CHALLENGE_METHOD}"
                if (IS_DEBUG) debugLog("RequestOAuth2AuthorizationCodeTask.run() : url = $url")

                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)

                getCodeActResLauncher.launch(intent)

                if (IS_DEBUG) debugLog("RequestOAuth2AuthorizationCodeTask.run() : X")
            }
        }

    }

    init {
        refreshTask = RefreshAccessTokenTask()

        refreshToken = PetApplication.getSP().getString(Constants.KEY_GCP_REFRESH_TOKEN, "")!!

    }

    fun refreshAccessToken() {
        backHandler.post(refreshTask)
        while (backHandler.hasCallbacks(refreshTask)) {
            Thread.sleep(100)
        }
    }

    private inner class RefreshAccessTokenTask : Runnable {
        private val TOKEN_URL = "https://oauth2.googleapis.com/token"

        override fun run() {
            if (IS_DEBUG) debugLog("RefreshAccessTokenTask.run() : E")

            val body = "client_id=${clientId}&grant_type=refresh_token&refresh_token=$refreshToken&redirect_uri=${Constants.GCP_REDIRECT_URI}"
            if (IS_DEBUG) debugLog("RefreshAccessTokenTask.run() : body = $body")

            val client = OkHttpClient()
            val requestBody = body.toRequestBody(Constants.GCP_CONTENT_TYPE)
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("RefreshAccessTokenTask.run() : Get access token OK")

                response.body.let {
                    val responseBody = it.string()

                    if (IS_DEBUG) {
                        debugLog("## RESPONSE")
                        debugLog(responseBody)
                    }

                    val mapper = jacksonObjectMapper()
                    val rootNode = mapper.readTree(responseBody)

                    Companion.accessToken = rootNode.get("access_token").toString()

                    if (IS_DEBUG) {
                        debugLog("## rootNode = $rootNode")
                        debugLog("## accessToken = ${Companion.accessToken}")
                    }
                }
            } else {
                errorLog("RefreshAccessTokenTask.run() : Get access-token NG")
                errorLog("## NG Response = $response")
                errorLog("## NG Response body = ${response.body}")
                errorLog("## NG Response headers = ${response.headers}")
                errorLog("## NG Response message = ${response.message}")
            }

            if (IS_DEBUG) debugLog("RefreshAccessTokenTask.run() : X")
        }
    }


    fun prepareToSpeak() {
        backHandler.post(GetVoiceListTask())
        backHandler.post(StartAudioTrackPlayerTask())
    }

    fun loadLabelVsPackage(callback: OnTtsEngineOptionLoadedCallback) {
        backHandler.post {
            val map: MutableMap<String, String> = mutableMapOf()
            voices.forEach { voice ->
                map[voice.name] = voice.name
            }

            callback.onLoaded(map)
        }
    }

    private inner class GetVoiceListTask : Runnable {
        private val GET_URL = "https://texttospeech.googleapis.com/v1/voices?languageCode=$LANG&access_token=$accessToken"

        override fun run() {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(GET_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("TTSController.GCP.GetVoiceListTask: OK")

                response.body?.let {
                    val responseBody = it.string()

                    if (IS_DEBUG) {
                        debugLog("#### RESPONSE")
                        debugLog(responseBody)
                    }

                    val mapper = jacksonObjectMapper()
                    val rootNode = mapper.readTree(responseBody)

                    val voiceList = rootNode.get("voices").toList()
                    if (voiceList.isNotEmpty()) {
                        voices.clear()
                        voiceList.forEach { voice ->
                            val name = voice.get("name").asText()
                            val ssmlGender = voice.get("ssmlGender").asText()
                            val naturalSampleRateHertz = voice.get("naturalSampleRateHertz").asInt()
                            voices.add(Voice(name, ssmlGender, naturalSampleRateHertz))
                        }

                        if (IS_DEBUG) {
                            debugLog("GCP TTS Voice added.")
                            voices.forEach { voice ->
                                debugLog("    voice = $voice")
                            }
                        }

                        if (option != Constants.VAL_DEFAULT) {
                            run loop@ {
                                voices.forEach { voice ->
                                    if (voice.name == option) {
                                        selectedVoice = voice
                                        return@loop
                                    }
                                }
                            }
                        }
                        if (IS_DEBUG) debugLog("Selected Voice = ${selectedVoice?.name}")
                    }
                }

            } else {
                if (IS_DEBUG) debugLog("TTSController.GCP.GetVoiceListTask: NG")
                errorLog("NG Response = $response")
            }
        }
    }

    private inner class StartAudioTrackPlayerTask : Runnable {
        override fun run() {
            val voice: Voice = selectedVoice ?: voices.first()

            playerBufSize = AudioTrack.getMinBufferSize(
                    voice.naturalSampleRateHertz,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
            if (IS_DEBUG) debugLog("MinBufSize = $playerBufSize")

            val p = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(voice.naturalSampleRateHertz)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(playerBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

            val frameCount = p.bufferSizeInFrames
            frameSizeInBytes = playerBufSize / frameCount

            p.play()

            player = p
        }
    }

    private inner class StopAudioTrackPlayerTask : Runnable {
        override fun run() {
            player?.stop()
            player?.release()
            player = null
        }
    }

    override fun release() {
        backHandler.removeCallbacks(refreshTask)
        backHandler.post(StopAudioTrackPlayerTask())

    }

    override fun speak(text: String) {
        isSpeaking = true

        backHandler.post { callback?.onSpeechStarted() }

        backHandler.post(RequestTtsTask(text, callback))

    }

    private inner class RequestTtsTask(
            val text: String,
            val callback: TTSController.Callback?) : Runnable {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val POST_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"

        private fun getJson(text: String, voice: Voice): String {
            return """
                {
                    "input":{
                        "text":"$text"
                    },
                    "voice":{
                        "languageCode":"$LANG",
                        "name":"${voice.name}",
                        "ssmlGender":"${voice.ssmlGender}"
                    },
                    "audioConfig":{
                        "audioEncoding":"LINEAR16",
                    }
                }
            """.trimIndent()
        }

        override fun run() {
            val voice: Voice = selectedVoice ?: voices.first()

            val client = OkHttpClient()
            val requestBody = getJson(text, voice).toRequestBody(CONTENT_TYPE)
            val request = Request.Builder()
                    .url(POST_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .post(requestBody)
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to GCP-TTS")
                debugLog("URL = $POST_URL")
                debugLog("Header = ${request.headers}")
                debugLog("Text = $text")
                debugLog("Body = ${getJson(text, voice)}")
            }

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("GCP-TTS: OK")

                response.body?.let {
                    val responseBody = it.string()

                    if (IS_DEBUG) {
                        debugLog("#### RESPONSE")
                        debugLog(responseBody)
                    }

                    val mapper = jacksonObjectMapper()
                    val rootNode = mapper.readTree(responseBody)

                    // Parse response.
                    val base64pcm16 = rootNode.get("audioContent").asText()
                    val pcm16Buffer: ByteArray = Base64.decode(base64pcm16, Base64.DEFAULT)

                    if (IS_DEBUG) {
                        debugLog("Audio Content BASE64 = $base64pcm16")
                        debugLog("Audio Buffer Size = ${pcm16Buffer.size}")
                        val rateBytes = byteArrayOf(
                                pcm16Buffer[27],
                                pcm16Buffer[26],
                                pcm16Buffer[25],
                                pcm16Buffer[24])
                        val rate = ByteBuffer.wrap(rateBytes).int
                        debugLog("WAVE Sample Rate = $rate")
                    }

                    val p = player
                    if (p != null) {
                        p.setPlaybackPositionUpdateListener(PlayDoneCallback())

                        // Steaming.
                        var offset = 44 // Depends on RIFF WAVE file format.

                        val frameCount = (pcm16Buffer.size - offset) / frameSizeInBytes

                        val ret = p.setNotificationMarkerPosition(frameCount)

                        if (IS_DEBUG) {
                            debugLog("Frame Count = $frameCount")
                            debugLog("Result of setNotificationMarkerPosition() = $ret")
                        }

                        do {
                            val writeSize = if (offset + playerBufSize > pcm16Buffer.size) {
                                pcm16Buffer.size - offset
                            } else {
                                playerBufSize
                            }

                            val writtenCount = p.write(pcm16Buffer, offset, writeSize)

                            if (IS_DEBUG) debugLog("Player written count = $writtenCount")

                            offset += writtenCount

                        } while (writtenCount > 0)

                    } else {
                        // Player is null, maybe already released.
                        errorLog("GCP-TTS : NG, Maybe already released.")
                        backHandler.post { callback?.onSpeechDone(false) }
                    }
                }

            } else {
                errorLog("GCP-TTS : NG")
                errorLog("NG Response = $response")
                backHandler.post { callback?.onSpeechDone(false) }
            }
        }

        private inner class PlayDoneCallback : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                if (IS_DEBUG) debugLog("TTSController.GCP.RequestTtsTask.PlayDoneCallback")

                isSpeaking = false
                callback?.onSpeechDone(true)
            }

            override fun onPeriodicNotification(track: AudioTrack?) {
                // NOP.
            }
        }
    }

}
