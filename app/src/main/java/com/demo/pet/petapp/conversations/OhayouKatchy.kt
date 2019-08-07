package com.demo.pet.petapp.conversations

import android.content.Context
import android.widget.Toast
import com.demo.pet.petapp.R
import com.demo.pet.petapp.debugLog

class OhayouKatchy(var context: Context?) : ConversationStrategy {
    private val keywords: List<String>

    init {
        val ctx = context
        if (ctx != null) {
            keywords = ctx.resources.getStringArray(R.array.ohayou_katchy_keywords).toList()
        } else {
            keywords = ArrayList()
        }
    }

    override fun getFilterKeywords(): List<String> {
        return keywords
    }

    override fun release() {
        context = null
    }

    override fun conversate(sentence: String, keywords: List<String>): String {
        val keyword = keywords.first()

        debugLog("## KEYWORD = $keyword is DETECTED")

        Toast.makeText(context, "KATCHY << $keyword", Toast.LENGTH_SHORT).show()

        val outword = when (keyword) {
            // Sphinx
            "ohayou" -> {
                "おはようございます"
            }
            "konnichiwa" -> {
                "こんばんわ"
            }

            // Google Speech API.
            "おはよう" -> {
                "おはようございます"
            }
            "こんにちは" -> {
                "こんにちわ"
            }

            else -> {
                "よくわかりません"
            }
        }

        return outword
    }

}
