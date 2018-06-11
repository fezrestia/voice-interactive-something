package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.debugLog

/**
 * Speech to Text implementation type.
 */
enum class STTType {
    ANDROID_SPEECH_RECOGNIZER,
    POCKET_SPHINX,
}

fun createSTTController(context: Context, type: STTType): STTController {
    return when (type) {
        STTType.ANDROID_SPEECH_RECOGNIZER -> {
            if (STTControllerAndroid.isSupported(context)) {
                STTControllerAndroid(context)
            } else {
                STTControllerUnAvailable()
            }
        }
        STTType.POCKET_SPHINX -> {
            STTControllerPocketSphinx(context)
        }
    }
}

/**
 * Speech to Text interface.
 */
interface STTController {
    fun registerKeywordFilter(keywords: List<String>, filter: KeywordFilterCallback)
    fun unregisterKeywordFilter(keywords: List<String>)
    fun clearKeywordFilter()

    fun ready()

    fun startRecog()
    fun stopRecog()
    fun isActive(): Boolean

    fun release()

    /**
     * Keyword filter interface for Speech2Text converter.
     */
    interface KeywordFilterCallback {
        /**
         * Keyword is detected.
         * @keyword
         */
        fun onDetected(keyword: String)
    }
}

private class STTControllerUnAvailable : STTController {
    override fun registerKeywordFilter(
            keywords: List<String>,
            filter: STTController.KeywordFilterCallback) {
        debugLog("Function UnAvailable")
    }

    override fun unregisterKeywordFilter(keywords: List<String>) {
        debugLog("Function UnAvailable")
    }

    override fun clearKeywordFilter() {
        debugLog("Function UnAvailable")
    }

    override fun ready() {
        debugLog("Function UnAvailable")
    }

    override fun startRecog() {
        debugLog("Function UnAvailable")
    }

    override fun stopRecog() {
        debugLog("Function UnAvailable")
    }

    override fun isActive(): Boolean {
        debugLog("Function UnAvailable")
        return false
    }

    override fun release() {
        debugLog("Function UnAvailable")
    }
}
