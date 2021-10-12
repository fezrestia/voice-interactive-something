@file:Suppress("PrivatePropertyName", "SimplifyBooleanWithConstants", "ConstantConditionIf")

package com.demo.pet.petapp.conversations

import android.os.Handler
import android.os.HandlerThread
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RecruitSmallTalk : ConversationStrategy {
    private val IS_DEBUG = Log.IS_DEBUG || false

    companion object {
        init {
            System.loadLibrary("config")
        }
    }

    private val backThread = HandlerThread("small-talk", Thread.NORM_PRIORITY)
    private val backHandler: Handler

    private val apiKey = getApiKey()

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)

    }

    override fun release() {
        backThread.quitSafely()

    }

    override fun getFilterKeywords(): List<String> {
        return listOf()
    }

    override fun conversate(sentence: String, keywords: List<String>): String {
        val task = PostMessageTask(sentence, null, null)
        backHandler.post(task)

        task.await()

        return task.resMsg
    }

    override fun asyncConversate(
            sentence: String,
            keywords: List<String>,
            callback: ConversationStrategy.Callback,
            callbackHandler: Handler?) {
        backHandler.post(PostMessageTask(sentence, callback, callbackHandler))
    }

    private inner class PostMessageTask(
            private val message: String,
            private val callback: ConversationStrategy.Callback?,
            private val callbackHandler: Handler?) : Runnable {
        private val POST_URL = "https://api.a3rt.recruit.co.jp/talk/v1/smalltalk"
        private val CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()

        private val countdown = CountDownLatch(1)

        var resMsg: String = ConversationStrategy.SILENT_RESPONSE

        private fun getBody(msg: String): String {
            return "apikey=${apiKey}&query=${msg}"
        }

        override fun run() {
            val client = OkHttpClient()
            val requestBody = getBody(message).toRequestBody(CONTENT_TYPE)
            val request = Request.Builder()
                    .url(POST_URL)
                    .post(requestBody)
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to Recruit Small Talk API")
                debugLog("URL = $POST_URL")
                debugLog("Header = ${request.headers}")
                debugLog("Message = $message")
                debugLog("Body = ${getBody(message)}")
            }

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("OK")

                response.body?.let {
                    val responseBody = it.string()

                    if (IS_DEBUG) {
                        debugLog("#### RESPONSE")
                        debugLog(responseBody)
                    }

                    val mapper = jacksonObjectMapper()
                    val rootNode = mapper.readTree(responseBody)

                    // Parse response.
                    val status = rootNode.get("status").asInt()
                    val message = rootNode.get("message").asText()
                    val reply = rootNode.get("results").first().get("reply").asText()

                    if (IS_DEBUG) {
                        debugLog("Res status  = $status")
                        debugLog("Res message = $message")
                        debugLog("Res reply   = $reply")
                    }

                    resMsg = reply
                }

            } else {
                errorLog("NG")
                errorLog("NG Response = $response")
            }

            // for blocking API.
            countdown.countDown()

            // for async API.
            if (callback != null) {
                val handler = callbackHandler ?: backHandler
                handler.post { callback.onCompleted(resMsg) }
            }

        }

        // for blocking API.
        fun await() {
            val isOk = countdown.await(5, TimeUnit.SECONDS)

            if (!isOk) {
                errorLog("ERROR: Timeout for request")
            }
        }
    }

    private external fun getApiKey(): String
}
