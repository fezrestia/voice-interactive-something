package com.demo.pet.petapp.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.demo.pet.petapp.Log
import com.demo.pet.petapp.debugLog

/**
 * Speech to Text function controller on Android API.
 */
class STTControllerAndroid(context: Context) : STTController {
    private val IS_DEBUG = true || Log.IS_DEBUG

    companion object {
        fun isSupported(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private val speechRecogIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private val speechRecog: SpeechRecognizer

    private val keywordVsFilter: MutableMap<String, STTController.KeywordFilterCallback> = HashMap()

    private var isActive = false

    init {
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecog = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecog.setRecognitionListener(SpeechRecognitionListenerImpl())

    }

    override fun registerKeywordFilter(
            keywords: List<String>,
            filter: STTController.KeywordFilterCallback) {
        keywords.forEach { keyword: String -> keywordVsFilter[keyword] = filter }
    }

    override fun unregisterKeywordFilter(keywords: List<String>) {
        keywords.forEach { keyword: String -> keywordVsFilter.remove(keyword) }
    }

    override fun clearKeywordFilter() {
        keywordVsFilter.clear()
    }

    override fun ready() {
        // NOP.
    }

    override fun release() {
        stopRecog()
        speechRecog.destroy()
        keywordVsFilter.clear()
    }

    private inner class SpeechRecognitionListenerImpl : RecognitionListener {
        override fun onBufferReceived(buffer: ByteArray?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onBufferReceived()")

        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onEvent()")
        }

        override fun onRmsChanged(rmsdB: Float) {
//            if (IS_DEBUG) debugLog("STTCtrl.Callback.onRmsChanged()")

        }

        override fun onError(error: Int) {

            when(error) {
                SpeechRecognizer.ERROR_AUDIO -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_AUDIO")
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_CLIENT")
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_INSUFFICIENT_PERMISSIONS")
                }
                SpeechRecognizer.ERROR_NETWORK -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_NETWORK")
                }
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_NETWORK_TIMEOUT")
                }
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_NO_MATCH")
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_RECOGNIZER_BUSY")
                }
                SpeechRecognizer.ERROR_SERVER -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_SERVER")
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (IS_DEBUG) debugLog("STTCtrl.Callback.onError() : ERROR_SPEECH_TIMEOUT")
                }
            }

            startRecog()

        }

        override fun onResults(results: Bundle?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onResults()")

            if (results != null) {
                val recogTexts: List<String> = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (IS_DEBUG) debugLog("Total Results:")
                for (text in recogTexts) {
                    if (IS_DEBUG) debugLog("    $text")
                }
            }

            startRecog()
        }

        override fun onEndOfSpeech() {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onEndOfSpeech()")

        }

        override fun onBeginningOfSpeech() {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onBeginningOfSpeech()")

        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onPartialResults()")

            if (partialResults != null) {
                val recogTexts: List<String> = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (IS_DEBUG) debugLog("Partial Results:")
                for (text in recogTexts) {
                    if (IS_DEBUG) debugLog("    $text")
                }

                keywordVsFilter.values.first().onDetected(recogTexts.first())

            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onReadyForSpeech()")
        }
    }

    override fun startRecog() {
        isActive = true
        speechRecog.startListening(speechRecogIntent)
    }

    override fun stopRecog() {
        speechRecog.stopListening()
        isActive = false
    }

    override fun isActive(): Boolean {
        return isActive
    }

}
