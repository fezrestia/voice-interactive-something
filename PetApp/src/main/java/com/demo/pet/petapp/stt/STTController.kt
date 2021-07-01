package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.Constants
import com.demo.pet.petapp.PetApplication
import com.demo.pet.petapp.util.debugLog

/**
 * Speech to Text implementation type.
 */
enum class STTType {
    ANDROID_SPEECH_RECOGNIZER,
    POCKET_SPHINX,
    GOOGLE_CLOUD_PLATFORM,
}

/**
 * Static factory method.
 */
fun createSTTController(
        context: Context,
        type: STTType,
        callback: STTController.Callback): STTController {
    val stt = when (type) {
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
        STTType.GOOGLE_CLOUD_PLATFORM -> {
            val speakThreshold = PetApplication.getSP().getInt(
                    Constants.KEY_SPEAK_THRESHOLD,
                    Constants.SPEAK_THRESHOLD_DEFAULT)
            STTControllerGoogleCloudApi(context, speakThreshold)
        }
    }
    stt.callback = callback
    return stt
}

/**
 * Speech to Text interface.
 */
interface STTController {
    /**
     * Listening callback interface.
     */
    interface Callback {
        /**
         * Indicates detection progress.
         */
        fun onDetecting(detecting: String)

        /**
         * Indicates detection result.
         */
        fun onDetected(sentence: String, keywords: List<String>)

        /**
         * Indicates sound recording is started.
         */
        fun onSoundRecStarted()

        /**
         * Indicates sound recording is stopped.
         */
        fun onSoundRecStopped()

        /**
         * Normalized sound level is changed between min and max.
         */
        fun onSoundLevelChanged(level: Int, min: Int, max: Int)
    }

    var callback: Callback?

    fun registerKeywords(keywords: List<String>)
    fun unregisterKeywords(keywords: List<String>)
    fun clearKeywords()

    fun prepare()

    fun startRecog()
    fun stopRecog()
    fun resumeRecog()
    fun pauseRecog()
    var isActive: Boolean

    fun release()

}

private class STTControllerUnAvailable : STTController {
    override var callback: STTController.Callback? = null
    override var isActive: Boolean = false

    override fun registerKeywords(keywords: List<String>) {
        debugLog("Function UnAvailable")
    }

    override fun unregisterKeywords(keywords: List<String>) {
        debugLog("Function UnAvailable")
    }

    override fun clearKeywords() {
        debugLog("Function UnAvailable")
    }

    override fun prepare() {
        debugLog("Function UnAvailable")
    }

    override fun startRecog() {
        debugLog("Function UnAvailable")
    }

    override fun stopRecog() {
        debugLog("Function UnAvailable")
    }

    override fun resumeRecog() {
        debugLog("Function UnAvailable")
    }

    override fun pauseRecog() {
        debugLog("Function UnAvailable")
    }

    override fun release() {
        debugLog("Function UnAvailable")
    }
}
