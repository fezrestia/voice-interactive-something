@file:Suppress("ConstantConditionIf")

package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.debugLog

/**
 * Speech to Text function controller on Android API.
 */
class STTControllerGoogleCloudApi(val context: Context) : STTController {
    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = true // Log.IS_DEBUG

    private var voiceRec: VoiceRecorder? = null
    private var webApi: GoogleSpeechApi? = null

    private val keywordVsFilter: MutableMap<String, STTController.KeywordFilterCallback> = HashMap()

    private var isActive = false

    init {
        val api = GoogleSpeechApi(context)
        api.callback = GoogleSpeechApiCallback()
        webApi = api

        val vr = VoiceRecorder(context)
        vr.callback = VoiceRecorderCallback()
        voiceRec = vr
    }

    override fun release() {
        webApi?.release()
        webApi = null

        voiceRec?.release()
        voiceRec = null
    }

    override fun registerKeywordFilter(
            keywords: List<String>,
            filter: STTController.KeywordFilterCallback) {
        keywords.forEach { keyword: String ->
            keywordVsFilter[keyword] = filter
        }
    }

    override fun unregisterKeywordFilter(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            keywordVsFilter.remove(keyword)
        }
    }

    override fun clearKeywordFilter() {
        keywordVsFilter.clear()
    }

    override fun ready() {
        // NOP.
    }

    override fun startRecog() {
        voiceRec?.start()
    }

    private inner class VoiceRecorderCallback : VoiceRecorder.Callback {
        override fun onStarted(samplingRate: Int) {
            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onStarted()")

            isActive = true
            webApi?.startRecog(samplingRate)

        }

        override fun onStopped() {
            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onStopped()")

            isActive = false
            webApi?.stopRecog()

        }

        override fun onRecorded(buffer: ByteArray, format: Int, size: Int) {
//            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onRecorded()")

            webApi?.recognize(buffer, size)
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

            if (isEnd) {
                keywordVsFilter.forEach { key, filter ->
                    if (text.contains(key)) {
                        filter.onDetected(key)
                    }
                }
            }
        }
    }

    override fun stopRecog() {
        voiceRec?.stop()
    }

    override fun isActive(): Boolean {
        return isActive
    }

}
