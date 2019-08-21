package com.demo.pet.petapp.tts

import android.content.Context

/**
 * Text to Speech implementation type.
 */
enum class TTSType {
    ANDROID,
}

/**
 * Static factory method.
 */
fun createTTSController(
        context: Context,
        type: TTSType,
        pkg: String,
        callback: TTSController.Callback): TTSController {
    val tts = when (type) {
        TTSType.ANDROID -> {
            TTSControllerAndroid(context, pkg)
        }
    }
    tts.callback = callback
    return tts
}

/**
 * Get engine options for each TTS engine.
 *
 * @param context
 * @param type
 * @param callback
 */
fun loadTTSEngineOptions(
        context: Context,
        type: TTSType,
        callback: OnTtsEngineOptionLoadedCallback) {
    return when (type) {
        TTSType.ANDROID -> {
            TTSControllerAndroid.loadLabelVsPackage(context, callback)
        }
//        else -> {
//            listOf("default")
//        }
    }
}

/**
 * Callback for loadig engine options.
 */
interface OnTtsEngineOptionLoadedCallback {
    fun onLoaded(labelVsPackage: Map<String, String>)
}



interface TTSController {
    /**
     * Callback interfaces.
     */
    interface Callback {
        fun onSpeechStarted()
        fun onSpeechDone(isSucceeded: Boolean)

    }

    var callback: Callback?

    var isSpeaking: Boolean

    /**
     * Release ALL references.
     */
    fun release()

    /**
     * Request to speak. Async API.
     */
    fun speak(text: String)

}
