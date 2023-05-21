@file:Suppress("SimplifyBooleanWithConstants", "PrivatePropertyName")

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

class Rinna : ConversationStrategy {
    private val IS_DEBUG = Log.IS_DEBUG || false

    companion object {
        init {
            System.loadLibrary("config")
        }
    }

    private val backThread = HandlerThread("rinna", Thread.NORM_PRIORITY)
    private val backHandler: Handler

    private val primaryApiKey = getPrimaryApiKey()

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
        callbackHandler: Handler?
    ) {
        backHandler.post(PostMessageTask(sentence, callback, callbackHandler))
    }

    private inner class PostMessageTask(
            private val message: String,
            private val callback: ConversationStrategy.Callback?,
            private val callbackHandler: Handler?) : Runnable {
        private val POST_URL = "https://api.rinna.co.jp/models/cce"
        private val TIMEOUT_MILLIS = 30000L
        private val JSON_MIME = "application/json; charaset=utf-8".toMediaTypeOrNull()
        private val RESPONSE_MESSAGE_MAX_TOKEN_LENGTH = 25 // Fixed by Rinna API spec.

        private val countdown = CountDownLatch(1)

        var resMsg: String = ConversationStrategy.SILENT_RESPONSE

        override fun run() {
            val client = OkHttpClient().newBuilder()
                    .connectTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .build()

            val bodyJson = "{\"rawInput\":\"${message}\",\"outputLength\":${RESPONSE_MESSAGE_MAX_TOKEN_LENGTH}}"

            val request = Request.Builder()
                    .url(POST_URL)
                    .header("Content-Type", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("Ocp-Apim-Subscription-Key", primaryApiKey)
                    .post(bodyJson.toRequestBody(JSON_MIME))
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to Rinna API")
                debugLog("URL = $POST_URL")
                debugLog("Header = ${request.headers}")
                debugLog("Message = $message")
                debugLog("Body = ${request.body}")
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
                    val answer = rootNode.get("answer").asText()

                    if (IS_DEBUG) {
                        debugLog("Res answer  = $answer")
                    }

                    resMsg = answer
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

    private external fun getPrimaryApiKey(): String
}
