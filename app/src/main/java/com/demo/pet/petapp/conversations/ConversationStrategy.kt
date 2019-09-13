package com.demo.pet.petapp.conversations

import android.content.Context
import android.os.Handler

enum class ConversationType {
    OHAYOU_KATCHY,
    USER_DEF,
    KATCHY_ONLINE,
    ECHO_BACK,
    RECRUIT_SMALL_TALK,
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
        ConversationType.KATCHY_ONLINE -> {
            KatchyOnline()
        }
        ConversationType.ECHO_BACK -> {
            EchoBack()
        }
        ConversationType.RECRUIT_SMALL_TALK -> {
            RecruitSmallTalk()
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

    interface Callback {
        fun onCompleted(response: String)
    }

    /**
     * Get input filtered keywords.
     */
    fun getFilterKeywords(): List<String>

    /**
     * Convert Request INPUT text to Response OUTPUT text.
     * This is blocking API.
     */
    fun conversate(sentence: String, keywords: List<String>): String

    /**
     * Convert request from INPUT text to OUTPUT text on background thread.
     * This is asynchronous API. Callback will be invoked on callbackHandler.
     *
     * @param sentence INPUT text.
     * @param keywords INPUT keywords
     * @param callback
     * @param callbackHandler If null, callback will be invoked on internal background thread.
     */
    fun asyncConversate(
            sentence: String,
            keywords: List<String>,
            callback: Callback,
            callbackHandler: Handler?)

    /**
     * Release ALL references.
     */
    fun release()

}
