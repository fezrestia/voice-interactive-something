@file:Suppress("ConstantConditionIf")

package com.demo.pet.petapp.stt

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.demo.pet.petapp.debugLog

/**
 * Speech to Text function controller on Android API.
 */
class STTControllerGoogleCloudApi(val context: Context, speakThreshold: Int) : STTController {
    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = true // Log.IS_DEBUG

    private var voiceRec: VoiceRecorder? = null
    private var webApi: GoogleSpeechApi? = null

    private val keywordVsFilter: MutableMap<String, STTController.KeywordFilterCallback> = HashMap()

    private var isActive = false

    var debugMsg: TextView? = null
    var voiceLevel: View? = null

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

            voiceLevel?.setBackgroundColor(Color.RED)
        }

        override fun onStopped() {
            if (IS_DEBUG) debugLog("VoiceRecorderCallback.onStopped()")

            isActive = false
            webApi?.stopRecog()

            voiceLevel?.setBackgroundColor(Color.WHITE)
        }

        override fun onSoundLevelChanged(level: Int, min: Int, max: Int) {
            // Debug.
            val target = voiceLevel
            if (target != null) {
                val rate = level.toFloat() / (max.toFloat() - min.toFloat())
                target.pivotX = 0.0f
                target.scaleX = rate
            }
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

            // Debug.
            debugMsg?.text = text

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
