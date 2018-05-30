package com.demo.pet.petapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Speech to Text function controller.
 */
class STTController(context: Context) {

    private val IS_DEBUG = true || Log.IS_DEBUG

    /**
     * Keyword filter interface for Speech2Text converter.
     */
    public interface KeywordFilterCallback {
        /**
         * Keyword is detected.
         */
        fun onDetected(keyword: String)
    }

    companion object {
        public fun isSupported(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private val speechRecogIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private val speechRecog: SpeechRecognizer

    private val keywordVsFilter: MutableMap<String, KeywordFilterCallback> = HashMap()

    private var isEnabled = true

    init {
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        speechRecogIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecog = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecog.setRecognitionListener(SpeechRecognitionListenerImpl())

    }

    public fun registerKeywordFilter(keywords: List<String>, filter: KeywordFilterCallback) {
        keywords.forEach { keyword: String -> keywordVsFilter[keyword] = filter }
    }

    public fun unregisterKeywordFilter(keywords: List<String>) {
        keywords.forEach { keyword: String -> keywordVsFilter.remove(keyword) }
    }

    public fun clearFilter() {
        keywordVsFilter.clear()
    }

    public fun pauseRecog() {
        if (IS_DEBUG) debugLog("recog PAUSE")
        isEnabled = false
    }

    public fun resumeRecog() {
        if (IS_DEBUG) debugLog("recog RESUME")
        isEnabled = true
    }

    public fun isResumed(): Boolean {
        return isEnabled
    }

    public fun release() {
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


                if (isEnabled) {
                    keywordVsFilter.values.first().onDetected(recogTexts.first())
                }



            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            if (IS_DEBUG) debugLog("STTCtrl.Callback.onReadyForSpeech()")
        }
    }

    public fun startRecog() {
        speechRecog.startListening(speechRecogIntent)
    }

    public fun stopRecog() {
        speechRecog.stopListening()
    }

}
