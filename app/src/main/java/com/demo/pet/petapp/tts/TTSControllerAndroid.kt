@file:Suppress("PrivatePropertyName", "ConstantConditionIf", "SimplifyBooleanWithConstants")

package com.demo.pet.petapp.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.demo.pet.petapp.Log
import com.demo.pet.petapp.debugLog

class TTSControllerAndroid(var context: Context?) : TTSController {
    private val IS_DEBUG = false || Log.IS_DEBUG

    override var callback: TTSController.Callback? = null

    override var isSpeaking: Boolean = false

    private val tts: TextToSpeech
    private val MAX_TEXT_SIZE = TextToSpeech.getMaxSpeechInputLength()
    private var sequenceId = 0

    init {
        if (IS_DEBUG) debugLog("TTSCtrl.init()")
        if (IS_DEBUG) debugLog("Init TTS with Default Engine.")
        tts = TextToSpeech(context, TTSOnInitCallback())
        tts.setOnUtteranceProgressListener(TTSOnProgressCallback())
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
            tts.voices.forEach { voice ->
                debugLog("    Voice = ${voice.name} / ${voice.locale.displayName}")
                voice.features.forEach { f ->
                    debugLog("        Feature = $f")
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

            callback?.onSpeechStarted()
        }

        override fun onError(utteranceId: String?) {
            // NOP. Deprecated.
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            if (IS_DEBUG) debugLog("TTSCtrl.Progress.onError()")

            isSpeaking = false
            callback?.onSpeechDone(false)
        }

        override fun onDone(utteranceId: String?) {
            if (IS_DEBUG) debugLog("TTSCtrl.Progress.onDone()")

            isSpeaking = false
            callback?.onSpeechDone(true)
        }
    }

    override fun release() {
        if (tts.isSpeaking) {
            tts.stop()
        }

        tts.shutdown()
    }

    override fun speak(text: String) {
        isSpeaking = true

        if (tts.isSpeaking) {
            tts.stop()
        }

        if (text.length > MAX_TEXT_SIZE) {
            if (IS_DEBUG) debugLog("TTSCtrl.speak() : Text is too long.")
        }

        if (IS_DEBUG) debugLog("TTSCtrl.speak() : text = $text")
        val ret = tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
               null,
                getNextSequenceId())
        if (IS_DEBUG) debugLog("TTSCtrl.speak() : done = $ret")
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
        return "${context?.packageName}-TTS-$sequenceId"
    }

}
