package com.demo.pet.petapp.conversations

import android.content.Context
import android.widget.Toast
import com.demo.pet.petapp.Constants
import com.demo.pet.petapp.MainActivity2.KeywordProtocol
import com.demo.pet.petapp.PetApplication
import com.demo.pet.petapp.debugLog
import com.demo.pet.petapp.stt.STTController

class UserDefinitions(val context: Context) : VoiceInteractionStrategy {
    private val keywordProtocols: Set<KeywordProtocol>
    private val detectTargets: List<String>
    private val keywordMap: Map<String, String>
    private var speakOutCallback: ((String) -> Unit)? = null

    init {
        val protocolSet = PetApplication.getSP().getStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                setOf<String>())

        keywordProtocols = mutableSetOf()
        keywordMap = mutableMapOf()

        protocolSet.forEach { protocol: String ->
            val keywords = protocol.split("=")
            keywordProtocols.add(KeywordProtocol(keywords[0], keywords[1]))
            keywordMap[keywords[0]] = keywords[1]
        }

        detectTargets = mutableListOf()
        keywordProtocols.forEach { protocol: KeywordProtocol ->
            detectTargets.add(protocol.inKeyword)
        }
    }

    override fun configureKeywordFilter(stt: STTController) {
        stt.registerKeywordFilter(detectTargets, KeywordFilterCallbackImpl())
    }

    override fun release(stt: STTController) {
        stt.unregisterKeywordFilter(detectTargets)
        speakOutCallback = null
    }

    private inner class KeywordFilterCallbackImpl : STTController.KeywordFilterCallback {
        override fun onDetected(keyword: String) {
            debugLog("## KEYWORD = $keyword is DETECTED")

            val outKeyword = keywordMap[keyword]
            if (outKeyword != null) {
                Toast.makeText(context, "KATCHY << $outKeyword", Toast.LENGTH_SHORT).show()
                speakOutCallback?.invoke(outKeyword)
            }
        }
    }

    override fun setSpeakOutRequestCallback(callback: (String) -> Unit) {
        speakOutCallback = callback
    }

}
