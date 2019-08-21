package com.demo.pet.petapp.stt

import android.content.Context
import com.demo.pet.petapp.util.debugLog
import edu.cmu.pocketsphinx.*

import java.io.File
import java.lang.Exception

/**
 * Speech to Text implementation with PocketSphinx.
 */
class STTControllerPocketSphinx(val context: Context) : STTController {
    private lateinit var recognizer: SpeechRecognizer
    private val keywords: MutableList<String> = ArrayList()

    override var callback: STTController.Callback? = null
    override var isActive: Boolean = false

    init {
        // NOP.
    }

    override fun prepare() {
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

    override fun registerKeywords(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            // Cache.
            if (!this.keywords.contains(keyword)) {
                this.keywords.add(keyword)
            }

            //TODO: Register recog target string to .keyword file in assets dir.

        }
    }

    override fun unregisterKeywords(keywords: List<String>) {
        keywords.forEach { keyword: String ->
            // Cache.
            this.keywords.remove(keyword)

            //TODO: Unregister recog target string from .keyword file in assets dir.

        }
    }

    override fun clearKeywords() {
        keywords.clear()

        //TODO: Clear recog target string in .keyword file in assets dir.

    }

    override fun startRecog() {
        isActive = true
        recognizer.startListening("keyword")
    }

    override fun stopRecog() {
        recognizer.cancel()
        recognizer.stop()
        isActive = false
    }

    private fun restartRecog() {
        stopRecog()
        startRecog()
    }

    override fun resumeRecog() {
        startRecog()
    }

    override fun pauseRecog() {
        stopRecog()
    }

    private inner class RecognitionListenerImpl : RecognitionListener {
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

            val phrase = hypothesis.hypstr
            debugLog("## PARTIAL DETECT = $phrase")

            keywords.forEach { keyword ->
                if (keyword == phrase) {
                    callback?.onDetected(phrase, listOf(keyword))
                }
            }

            restartRecog()
        }

        override fun onTimeout() {
            debugLog("onTimeout()")

            restartRecog()
        }

        override fun onBeginningOfSpeech() {
            debugLog("onBeginningOfSpeech()")
        }

        override fun onEndOfSpeech() {
            debugLog("onEndOfSpeech()")
        }

        override fun onError(error: Exception?) {
            debugLog("onError()")
            debugLog("ERR = ${error?.message}")

            restartRecog()
        }
    }

}
