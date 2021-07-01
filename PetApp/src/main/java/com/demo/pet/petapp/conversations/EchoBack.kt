package com.demo.pet.petapp.conversations

import android.os.Handler
import android.os.Looper

class EchoBack : ConversationStrategy {
    override fun getFilterKeywords(): List<String> {
        return listOf()
    }

    override fun conversate(sentence: String, keywords: List<String>): String {
        return sentence
    }

    override fun asyncConversate(
            sentence: String,
            keywords: List<String>,
            callback: ConversationStrategy.Callback,
            callbackHandler: Handler?) {
        val handler = callbackHandler ?: Handler(Looper.getMainLooper())
        handler.post { callback.onCompleted(sentence) }
    }

    override fun release() {
        // NOP.
    }
}
