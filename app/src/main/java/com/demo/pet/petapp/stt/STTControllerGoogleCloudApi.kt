@file:Suppress(
        "ConstantConditionIf",
        "SimplifyBooleanWithConstants")

package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog

/**
 * Speech to Text function controller on Android API.
 */
class STTControllerGoogleCloudApi(val context: Context, speakThreshold: Int) : STTController {
    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = true || Log.IS_DEBUG

    private var voiceRec: VoiceRecorder? = null
    private var webApi: GoogleSpeechApi? = null

    private val keywords: MutableList<String> = ArrayList()

    override var callback: STTController.Callback? = null
    override var isActive: Boolean = false

    private var isPaused = false

    init {
        val api = GoogleSpeechApi(context)
        api.callback = GoogleSpeechApiCallback()
        webApi = api

        val vr = VoiceRecorder(context, speakThreshold)
        vr.callback = VoiceRecorderCallback()
        voiceRec = vr
    }

    override fun release() {
        webApi?.release()
        webApi = null

        voiceRec?.release()
        voiceRec = null
    }

    override fun registerKeywords(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            if (!this.keywords.contains(keyword)) {
                this.keywords.add(keyword)
            }
        }
    }

    override fun unregisterKeywords(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            this.keywords.remove(keyword)
        }
    }

    override fun clearKeywords() {
        keywords.clear()
    }

    override fun prepare() {
        // NOP.
    }

    override fun startRecog() {
        voiceRec?.start()
    }

    override fun stopRecog() {
        voiceRec?.stop()
    }

    override fun resumeRecog() {
        isPaused = false
    }

    override fun pauseRecog() {
        isPaused = true
    }

    private inner class VoiceRecorderCallback : VoiceRecorder.Callback {
        override fun onStarted(samplingRate: Int) {
            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onStarted()")

            isActive = true
            webApi?.startRecog(samplingRate)

            callback?.onSoundRecStarted()
        }

        override fun onStopped() {
            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onStopped()")

            isActive = false
            webApi?.stopRecog()

            callback?.onSoundRecStopped()
        }

        override fun onSoundLevelChanged(level: Int, min: Int, max: Int) {
            callback?.onSoundLevelChanged(level, min, max)
        }

        override fun onRecorded(buffer: ByteArray, format: Int, size: Int) {
//            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onRecorded()")

            if (!isPaused) {
                webApi?.recognize(buffer, size)
            }
        }
    }

    private inner class GoogleSpeechApiCallback : GoogleSpeechApi.Callback {
        override fun onSpeechRecognized(text: String, isEnd: Boolean) {
            if (IS_DEBUG) debugLog("GoogleSpeechApiCallback.onSpeechRecognized()")
            if (isEnd) {
                if (IS_DEBUG) debugLog("RECOG FINISHED = $text")
            } else {
                if (IS_DEBUG) debugLog("RECOG = $text")
            }

            callback?.onDetecting(text)

            if (isEnd) {
                val detectedKeywords: MutableList<String> = ArrayList()
                keywords.forEach { keyword ->
                    if (text.contains(keyword)) {
                        detectedKeywords.add(keyword)
                    }
                }
                callback?.onDetected(text, detectedKeywords)
            }
        }
    }

}
