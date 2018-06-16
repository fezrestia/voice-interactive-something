package com.demo.pet.petapp.conversations

import android.content.Context
import android.widget.Toast
import com.demo.pet.petapp.R
import com.demo.pet.petapp.stt.STTController
import com.demo.pet.petapp.debugLog

class OhayouKatchy(val context: Context) : VoiceInteractionStrategy {
    private val keywords: List<String> = context.resources.getStringArray(R.array.ohayou_katchy_keywords).toList()
    private val filterCallback = KeywordFilterCallbackImpl()
    private var speakOutCallback: ((String) -> Unit)? = null

    override fun configureKeywordFilter(stt: STTController) {
        stt.registerKeywordFilter(keywords, filterCallback)
    }

    override fun release(stt: STTController) {
        stt.unregisterKeywordFilter(keywords)
        speakOutCallback = null
    }

    private inner class KeywordFilterCallbackImpl : STTController.KeywordFilterCallback {
        override fun onDetected(keyword: String) {

            debugLog("## KEYWORD = $keyword is DETECTED")

            Toast.makeText(context, "KATCHY << $keyword", Toast.LENGTH_SHORT).show()

            val outword = when (keyword) {
                "ohayou" -> {
                    "おはようございます"
                }
                "konnichiwa" -> {
                    "こんばんわ"
                }
                else -> {
                    "よくわかりません"
                }
            }

            val speakOut = speakOutCallback
            speakOut?.invoke(outword)
        }
    }

    override fun setSpeakOutRequestCallback(callback: (String) -> Unit) {
        speakOutCallback = callback
    }

}
