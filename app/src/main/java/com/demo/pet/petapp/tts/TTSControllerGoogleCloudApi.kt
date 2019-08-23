@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import com.demo.pet.petapp.Constants
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import java.nio.ByteBuffer

class TTSControllerGoogleCloudApi(var context: Context?, val option: String) : TTSController {
    private val IS_DEBUG = Log.IS_DEBUG || false

    override var callback: TTSController.Callback? = null
    override var isSpeaking: Boolean = false

    private val backThread = HandlerThread("gcp-tts", Thread.NORM_PRIORITY)
    private val backHandler: Handler

    private val refreshTask: RefreshAccessTokenTask

    private val LANG = "ja-JP"

    private data class Voice(
            val name: String,
            val ssmlGender: String,
            val naturalSampleRateHertz: Int)
    private val voices: MutableList<Voice> = mutableListOf()
    private var selectedVoice: Voice? = null // null means default

    private val id = getId()
    private val sec = getSec()
    private val refresh = getRefresh()

    private var accessToken: String = ""

    private var player: AudioTrack? = null
    private var playerBufSize: Int = 0
    private var frameSizeInBytes: Int = 0

    companion object {
        init {
            System.loadLibrary("config")
        }
    }

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)

        refreshTask = RefreshAccessTokenTask()
        backHandler.post(refreshTask)

        backHandler.post(GetVoiceListTask())

        backHandler.post(StartAudioTrackPlayerTask())

    }

    private inner class RefreshAccessTokenTask : Runnable {
        private val CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")
        private val BODY = "refresh_token=$refresh&client_id=$id&client_secret=$sec&grant_type=refresh_token"
        private val REFRESH_URL = "https://www.googleapis.com/oauth2/v4/token"

        override fun run() {
            val client = OkHttpClient()
            val requestBody = RequestBody.create(CONTENT_TYPE, BODY)
            val request = Request.Builder()
                    .url(REFRESH_URL)
                    .post(requestBody)
                    .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("TTSController.GCP.RefreshAccessTokenTask: OK")

                val responseBody: String = response.body().string()

                if (IS_DEBUG) {
                    debugLog("#### RESPONSE")
                    debugLog(responseBody)
                }

                val mapper = jacksonObjectMapper()
                val rootNode = mapper.readTree(responseBody)

                // Parse access token.
                accessToken = rootNode.get("access_token").asText()

                // Parse expiration timeout and register next refresh task.
                if (backThread.isAlive) {
                    val expireSec: Long = rootNode.get("expires_in").asLong()
                    backHandler.postDelayed(this, expireSec * 1000)
                }

            } else {
                if (IS_DEBUG) debugLog("TTSController.GCP.RefreshAccessTokenTask: NG")
                errorLog("NG Response = $response")
            }
        }
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
        private val GET_URL = "https://texttospeech.googleapis.com/v1/voices?languageCode=$LANG"

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

                val responseBody: String = response.body().string()

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
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
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

        backThread.quitSafely()

    }

    override fun speak(text: String) {
        isSpeaking = true

        backHandler.post { callback?.onSpeechStarted() }

        backHandler.post(RequestTtsTask(text, callback))

    }

    private inner class RequestTtsTask(
            val text: String,
            val callback: TTSController.Callback?) : Runnable {
        private val CONTENT_TYPE: MediaType = MediaType.parse("application/json; charset=utf-8")
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
            val requestBody = RequestBody.create(CONTENT_TYPE, getJson(text, voice))
            val request = Request.Builder()
                    .url(POST_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .post(requestBody)
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to GCP-TTS")
                debugLog("URL = $POST_URL")
                debugLog("Header = ${request.headers()}")
                debugLog("Text = $text")
                debugLog("Body = ${getJson(text, voice)}")
            }

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("GCP-TTS: OK")

                val responseBody: String = response.body().string()

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
                    if (IS_DEBUG) debugLog("WAVE Sample Rate = $rate")
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

    private external fun getId(): String
    private external fun getSec(): String
    private external fun getRefresh(): String
}
