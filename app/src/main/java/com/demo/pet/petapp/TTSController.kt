package com.demo.pet.petapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TTSController(val mContext: Context) {

    private val tts: TextToSpeech
    private val MAX_TEXT_SIZE = TextToSpeech.getMaxSpeechInputLength()
    private var sequenceId = 0

    init {
        debugLog("TTSCtrl.init()")

        tts = TextToSpeech(mContext, TTSOnInitCallback())
        tts.setOnUtteranceProgressListener(TTSOnProgressCallback())
    }

    private inner class TTSOnInitCallback : TextToSpeech.OnInitListener {
        override fun onInit(status: Int) {
            when (status) {
                TextToSpeech.SUCCESS -> {
                    debugLog("TTSCtrl.onInit() : SUCCESS")

                    checkCapability()
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

        debugLog("Default Lang = ${tts.defaultVoice.locale.displayName}")
        debugLog("Available Languages:")
        tts.availableLanguages.forEach {
            debugLog("    Lang = ${it.displayName}")
        }

        debugLog("Default Voice = ${tts.defaultVoice.name}")
        debugLog("Voices:")
        tts.voices.forEach {
            debugLog("    Voice = ${it.name} / ${it.locale.displayName}")
            it.features.forEach {
//                debugLog("        Feature = ${it}")
            }
        }
    }

    private fun configure() {
//        tts.language = Locale.JAPANESE

    }

    private inner class TTSOnProgressCallback : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            debugLog("TTSCtrl.Progress.onStart()")
        }

        override fun onError(utteranceId: String?) {
            // NOP.
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            debugLog("TTSCtrl.Progress.onError()")
        }

        override fun onDone(utteranceId: String?) {
            debugLog("TTSCtrl.Progress.onDone()")
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
            debugLog("TTSCtrl.speak() : Text is too long.")
        }

        val ret = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, getNextSequenceId())

        when (ret) {
            TextToSpeech.SUCCESS -> {
                debugLog("TTSCtrl.speak() : SUCCESS")
            }

            TextToSpeech.ERROR -> {
                debugLog("TTSCtrl.speak() : ERROR")
            }
        }
    }

    private fun getNextSequenceId(): String {
        ++sequenceId
        return "${mContext.packageName}-TTS-$sequenceId"
    }

}
