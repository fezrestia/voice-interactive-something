package com.demo.pet.petapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/**
 * Text to Speech implementation type.
 */
enum class TTSType {
    ANDROID,
}

class TTSController(
        private val context: Context,
        private var ttsType: TTSType,
        private var subType: String?,
        private var stateCallback: SpeakStateCallback?) {
    private val IS_DEBUG = false || Log.IS_DEBUG

    private val tts: TextToSpeech
    private val MAX_TEXT_SIZE = TextToSpeech.getMaxSpeechInputLength()
    private var sequenceId = 0

    companion object {
        val DEFAULT_ENGINE = "default"

        fun getSupportedEngines(context: Context): List<TextToSpeech.EngineInfo> {
            val tts = TextToSpeech(context, null)
            val supported = tts.engines
            tts.shutdown()
            return supported
        }
    }

    // Default engine with no callback.
    constructor(context: Context) : this(context, TTSType.ANDROID, null, null)
    // Specific engine with no callback.
    constructor(context: Context, ttsEngine: String) : this(context, TTSType.ANDROID, ttsEngine, null)

    init {
        if (IS_DEBUG) debugLog("TTSCtrl.init()")

        when (ttsType) {
            TTSType.ANDROID -> {
                if (subType == null) {
                    if (IS_DEBUG) debugLog("Init TTS with Default Engine.")
                    tts = TextToSpeech(context, TTSOnInitCallback())
                } else {
                    if (IS_DEBUG) debugLog("Init TTS with Engine = $subType")
                    tts = TextToSpeech(context, TTSOnInitCallback(), subType)
                }
            }
        }

        tts.setOnUtteranceProgressListener(TTSOnProgressCallback())
    }

    public interface SpeakStateCallback {
        fun onStarted()
        fun onCompleted(isSucceeded: Boolean)
    }

    private inner class TTSOnInitCallback : TextToSpeech.OnInitListener {
        override fun onInit(status: Int) {
            when (status) {
                TextToSpeech.SUCCESS -> {
                    if (IS_DEBUG) debugLog("TTSCtrl.onInit() : SUCCESS")

                    if (IS_DEBUG) checkCapability()
                    configure()
                }

                TextToSpeech.ERROR -> {
                    debugLog("TTSCtrl.onInit() : ERROR")
                }
            }
        }
    }

    private fun checkCapability() {
        debugLog("MAX_TEXT_SIZE = $MAX_TEXT_SIZE")

        debugLog("Default Engine = ${tts.defaultEngine}")
        debugLog("Installed Engines:")
        tts.engines.forEach {
            debugLog("    Engine = ${it.label}")
        }

        try {
            val defaultVoice = tts.defaultVoice
            debugLog("Default Lang = ${tts.defaultVoice?.locale?.displayName}")
            debugLog("Available Languages:")
            tts.availableLanguages.forEach {
                debugLog("    Lang = ${it.displayName}")
            }
        } catch (e: NullPointerException) {
            debugLog("NPE")
            e.printStackTrace()
        }

        try {
            debugLog("Default Voice = ${tts.defaultVoice.name}")
            debugLog("Voices:")
            tts.voices.forEach {
                debugLog("    Voice = ${it.name} / ${it.locale.displayName}")
                it.features.forEach {
                    debugLog("        Feature = ${it}")
                }
            }
        } catch (e: NullPointerException) {
            debugLog("NPE")
            e.printStackTrace()
        }
    }

    private fun configure() {
//        tts.language = Locale.JAPANESE

    }

    private inner class TTSOnProgressCallback : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (IS_DEBUG) debugLog("TTSCtrl.Progress.onStart()")

            stateCallback?.onStarted()
        }

        override fun onError(utteranceId: String?) {
            // NOP.
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            if (IS_DEBUG) debugLog("TTSCtrl.Progress.onError()")

            stateCallback?.onCompleted(false)
        }

        override fun onDone(utteranceId: String?) {
            if (IS_DEBUG) debugLog("TTSCtrl.Progress.onDone()")

            stateCallback?.onCompleted(true)
        }
    }

    public fun release() {
        if (tts.isSpeaking) {
            tts.stop()
        }

        tts.shutdown()
    }

    public fun speak(text: String) {
        if (tts.isSpeaking) {
            tts.stop()
        }

        if (text.length > MAX_TEXT_SIZE) {
            if (IS_DEBUG) debugLog("TTSCtrl.speak() : Text is too long.")
        }

        if (IS_DEBUG) debugLog("TTSCtrl.speak() : text = $text")
        val ret = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, getNextSequenceId())

        when (ret) {
            TextToSpeech.SUCCESS -> {
                if (IS_DEBUG) debugLog("TTSCtrl.speak() : SUCCESS")
            }

            TextToSpeech.ERROR -> {
                debugLog("TTSCtrl.speak() : ERROR")
            }
        }
    }

    private fun getNextSequenceId(): String {
        ++sequenceId
        return "${context.packageName}-TTS-$sequenceId"
    }

}
