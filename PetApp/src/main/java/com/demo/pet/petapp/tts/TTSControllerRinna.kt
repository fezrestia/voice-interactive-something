@file:Suppress("SameParameterValue", "PrivatePropertyName", "PropertyName", "SimplifyBooleanWithConstants")

package com.demo.pet.petapp.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class TTSControllerRinna(val voiceId: Int) : TTSController {
    private val IS_DEBUG = Log.IS_DEBUG || false

    override var callback: TTSController.Callback? = null
    override var isSpeaking: Boolean = false

    private val backThread = HandlerThread("rinna-tts", Thread.NORM_PRIORITY)
    private val backHandler: Handler

    private val primaryApiKey = getPrimaryApiKey()

    private var player: AudioTrack? = null
    private var playerBufSize: Int = 0
    private var frameSizeInBytes: Int = 0

    companion object {
        init {
            System.loadLibrary("config")
        }

        fun getVoiceLabelVsVoiceId(): Map<String, String> {
            val map: MutableMap<String, String> = mutableMapOf()
            for (i in 27..47) {
                map["VoiceID=$i"] = "$i"
            }
            return map
        }

    }

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)

        backHandler.post(StartAudioTrackPlayerTask())

    }

    override fun release() {
        backHandler.post(StopAudioTrackPlayerTask())

        backThread.quitSafely()

    }

    private inner class StartAudioTrackPlayerTask : Runnable {
        val SAMPLE_RATE = 24000

        override fun run() {
            playerBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
            if (IS_DEBUG) debugLog("MinBufSize = $playerBufSize")

            val p = AudioTrack.Builder()
                    .setAudioAttributes(
                            AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build())
                    .setAudioFormat(
                            AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(SAMPLE_RATE)
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

    override fun speak(text: String) {
        isSpeaking = true

        backHandler.post { callback?.onSpeechStarted() }

        backHandler.post(RequestTtsTask(text, callback))

    }

    private inner class RequestTtsTask(
            val text: String,
            val callback: TTSController.Callback?) : Runnable {
        private val POST_URL = "https://api.rinna.co.jp/models/cttse/v2"
        private val TIMEOUT_MILLIS = 30000L
        private val JSON_MIME = "application/json; charaset=utf-8".toMediaTypeOrNull()
        private val DEFAULT_SPEED = 1
        private val DEFAULT_VOLUME = 10
        private val DEFAULT_FORMAT = "wav"

        private fun getRequestJson(
                sid: Int,
                tid: Int,
                speed: Int,
                text: String,
                volume: Int,
                format: String) : String {
            return """
                {
                    "sid": $sid,
                    "tid": $tid,
                    "speed": $speed,
                    "text": "$text",
                    "volume": $volume,
                    "format": "$format"
                }
            """.trimIndent()
        }

        private fun downloadWave(client: OkHttpClient, url: String) : ByteBuffer {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body
            return if (body == null) {
                if (IS_DEBUG) debugLog("Failed to download WAV.")
                ByteBuffer.wrap(byteArrayOf())
            } else {
                ByteBuffer.wrap(body.bytes())
            }
        }

        override fun run() {
            val client = OkHttpClient().newBuilder()
                    .connectTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .build()

            val bodyJson = getRequestJson(
                    voiceId,
                    1, // emotional
                    DEFAULT_SPEED,
                    text,
                    DEFAULT_VOLUME,
                    DEFAULT_FORMAT)

            val request = Request.Builder()
                    .url(POST_URL)
                    .header("Content-Type", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("Ocp-Apim-Subscription-Key", primaryApiKey)
                    .post(bodyJson.toRequestBody(JSON_MIME))
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to Rinna TTS")
                debugLog("URL = $POST_URL")
                debugLog("Header = ${request.headers}")
                debugLog("Body = $bodyJson")
            }

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("Rinna-TTS: OK")

                response.body?.also {
                    val responseBody = it.string()

                    if (IS_DEBUG) {
                        debugLog("#### RESPONSE")
                        debugLog(responseBody)
                    }

                    val mapper = jacksonObjectMapper()
                    val rootNode = mapper.readTree(responseBody)

                    // Parse response.
                    val mediaContentUrl = rootNode.get("mediaContentUrl").asText()

                    if (IS_DEBUG) {
                        debugLog("mediaContentUrl = $mediaContentUrl")
                    }

                    // Get wave stream.
                    val wavByteBuffer: ByteBuffer = downloadWave(client, mediaContentUrl)
                    val wavSize = wavByteBuffer.limit()

                    val p = player
                    if (p != null) {
                        p.setPlaybackPositionUpdateListener(PlayDoneCallback())

                        val offset = 44 // Depends on RIFF WAVE file format.
                        val frameCount = (wavSize - offset) / frameSizeInBytes
                        val ret = p.setNotificationMarkerPosition(frameCount)

                        if (IS_DEBUG) {
                            debugLog("Frame Count = $frameCount")
                            debugLog("Result of setNotificationMarkerPosition() = $ret")
                        }

                        val writtenCount = p.write(wavByteBuffer, wavSize, AudioTrack.WRITE_BLOCKING)

                        if (IS_DEBUG) debugLog("Player written count = $writtenCount")

                    } else {
                        // Player is null, maybe already released.
                        errorLog("Rinna-TTS : NG, Maybe already released.")
                        backHandler.post { callback?.onSpeechDone(false) }
                    }
                }

            } else {
                errorLog("Rinna-TTS : NG")
                errorLog("NG Response = $response")
                backHandler.post { callback?.onSpeechDone(false) }
            }
        }

        private inner class PlayDoneCallback : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                if (IS_DEBUG) debugLog("TTSController.Rinna.RequestTtsTask.PlayDoneCallback")

                isSpeaking = false
                callback?.onSpeechDone(true)
            }

            override fun onPeriodicNotification(track: AudioTrack?) {
                // NOP.
            }
        }
    }

    private external fun getPrimaryApiKey(): String
}