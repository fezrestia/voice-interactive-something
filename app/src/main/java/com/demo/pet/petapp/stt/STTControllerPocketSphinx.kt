package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.debugLog
import edu.cmu.pocketsphinx.*

import java.io.File
import java.lang.Exception

/**
 * Speech to Text implementation with PocketSphinx.
 */
class STTControllerPocketSphinx(val context: Context) : STTController {
    private lateinit var recognizer: SpeechRecognizer
    private val keywordVsFilter: MutableMap<String, STTController.KeywordFilterCallback> = HashMap()
    private var isActive = false

    init {
        // NOP.
    }

    override fun ready() {
        val assets = Assets(context)
        val assetsDir = assets.syncAssets()

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetsDir, "en-us-ptm"))
//                .setDictionary(File(assetsDir, "cmudict-en-us.dict"))
                .setDictionary(File(assetsDir, "katchy.dict"))
//                .setRawLogDir(assetsDir)
                .recognizer

        val keywords = File(assetsDir, "katchy.keyword")
        recognizer.addKeywordSearch("keyword", keywords)

        recognizer.addListener(RecognitionListenerImpl())
    }

    override fun release() {
        recognizer.cancel()
        recognizer.shutdown()
    }

    override fun registerKeywordFilter(
            keywords: List<String>,
            filter: STTController.KeywordFilterCallback) {
        keywords.forEach { keyword: String ->
            // Cache.
            keywordVsFilter[keyword] = filter

            //TODO: Register recog target string to .keyword file in assets dir.

        }
    }

    override fun unregisterKeywordFilter(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            // Cache.
            keywordVsFilter.remove(keyword)

            //TODO: Unregister recog target string from .keyword file in assets dir.

        }
    }

    override fun clearKeywordFilter() {
        keywordVsFilter.clear()

        //TODO: Clear recog target string in .keyword file in assets dir.

    }

    override fun startRecog() {
        isActive = true
        recognizer.startListening("keyword")
    }

    override fun stopRecog() {
        recognizer.stop()
        isActive = false
    }

    private fun restartRecog() {
        stopRecog()
        startRecog()
    }

    override fun isActive(): Boolean {
        return isActive
    }

    private inner class RecognitionListenerImpl : RecognitionListener {
        private var isSpeaking = false

        override fun onResult(hypothesis: Hypothesis?) {
            if (hypothesis == null) {
                return
            }

            val phrase = hypothesis.hypstr

            debugLog("## DETECT = $phrase")

            restartRecog()
        }

        override fun onPartialResult(hypothesis: Hypothesis?) {
            if (hypothesis == null) {
                return
            }
            if (!isSpeaking) {
                debugLog("onPartialResult() : NOT on Speaking")
                return
            }

            val phrase = hypothesis.hypstr

            debugLog("## PARTIAL DETECT = $phrase")

        }

        override fun onTimeout() {
            debugLog("onTimeout()")

            restartRecog()
        }

        override fun onBeginningOfSpeech() {
            debugLog("onBeginningOfSpeech()")

            isSpeaking = true
        }

        override fun onEndOfSpeech() {
            debugLog("onEndOfSpeech()")

            isSpeaking = false
            restartRecog()
        }

        override fun onError(error: Exception?) {
            debugLog("onError()")
            debugLog("ERR = ${error?.message}")

            restartRecog()
        }
    }

}
