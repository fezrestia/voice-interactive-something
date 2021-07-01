package com.demo.pet.petapp.tts

import android.content.Context
import com.demo.pet.petapp.Constants

/**
 * Text to Speech implementation type.
 */
enum class TTSType {
    ANDROID,
    GOOGLE_CLOUD_PLATFORM,
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
        TTSType.GOOGLE_CLOUD_PLATFORM -> {
            TTSControllerGoogleCloudApi(context, pkg)
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
    when (type) {
        TTSType.ANDROID -> {
            TTSControllerAndroid.loadLabelVsPackage(context, callback)
        }
        TTSType.GOOGLE_CLOUD_PLATFORM -> {
            val tts = TTSControllerGoogleCloudApi(context, Constants.VAL_DEFAULT)
            tts.loadLabelVsPackage( object : OnTtsEngineOptionLoadedCallback {
                override fun onLoaded(labelVsPackage: Map<String, String>) {
                    tts.release()
                    callback.onLoaded(labelVsPackage)
                }
            } )
        }
//        else -> {
//            val defaultMap = mapOf(Pair(Constants.VAL_DEFAULT, Constants.VAL_DEFAULT))
//            Handler().post { callback.onLoaded(defaultMap) }
//        }
    }
}

/**
 * Callback for loading engine options.
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
