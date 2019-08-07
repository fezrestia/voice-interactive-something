package com.demo.pet.petapp.conversations

import android.content.Context

enum class ConversationType {
    OHAYOU_KATCHY,
    USER_DEF,
}

fun createConversationStrategy(
        context: Context,
        type: ConversationType) : ConversationStrategy {
    return when(type) {
        ConversationType.OHAYOU_KATCHY -> {
            OhayouKatchy(context)
        }
        ConversationType.USER_DEF -> {
            UserDefinitions(context)
        }
    }
}

/**
 * Conversation strategy interface.
 */
interface ConversationStrategy {

    companion object {
        /**
         * If conversation input can not be handled, silence is response.
         */
        const val SILENT_RESPONSE: String = ""
    }

    /**
     * Get input filtered keywords.
     */
    fun getFilterKeywords(): List<String>

    /**
     * Convert Request INPUT text to Response OUTPUT text.
     */
    fun conversate(sentence: String, keywords: List<String>): String

    /**
     * Release ALL references.
     */
    fun release()

}
