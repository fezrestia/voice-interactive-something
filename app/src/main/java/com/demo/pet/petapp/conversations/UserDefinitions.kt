package com.demo.pet.petapp.conversations

import android.content.Context
import android.widget.Toast
import com.demo.pet.petapp.Constants
import com.demo.pet.petapp.MainActivity2.KeywordProtocol
import com.demo.pet.petapp.PetApplication
import com.demo.pet.petapp.debugLog

class UserDefinitions(var context: Context?) : ConversationStrategy {
    private val keywordProtocols: Set<KeywordProtocol>
    private val detectTargets: List<String>
    private val keywordMap: Map<String, String>
    private var speakOutCallback: ((String) -> Unit)? = null

    init {
        val protocolSet = PetApplication.getSP().getStringSet(
                Constants.KEY_KEYWORD_PROTOCOLS,
                setOf<String>()) as Set<String>

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

    override fun getFilterKeywords(): List<String> {
        return keywordMap.keys.toList()
    }

    override fun release() {
        context = null
    }

    override fun conversate(sentence: String, keywords: List<String>): String {
        if (keywords.isEmpty()) {
            debugLog("## KEYWORD is EMPTY")
            return ConversationStrategy.SILENT_RESPONSE
        }

        val keyword = keywords.first()

        debugLog("## KEYWORD = $keyword is DETECTED")

        val outKeyword = keywordMap[keyword]

        return if (outKeyword != null) {
            Toast.makeText(context, "KATCHY << $outKeyword", Toast.LENGTH_SHORT).show()
            speakOutCallback?.invoke(outKeyword)

            outKeyword
        } else {
            "invalid"
        }
    }

}
