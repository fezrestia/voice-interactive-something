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
        callback: TTSController.Callback): TTSController {
    val tts = when (type) {
        TTSType.ANDROID -> {
            TTSControllerAndroid(context)
        }
    }
    tts.callback = callback
    return tts
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
