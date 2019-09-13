@file:Suppress(
        "PrivatePropertyName",
        "ConstantConditionIf",
        "SimplifyBooleanWithConstants")

package com.demo.pet.petapp.conversations

import android.os.Handler
import android.os.HandlerThread
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KatchyOnline : ConversationStrategy {
    private val IS_DEBUG = Log.IS_DEBUG || false

    companion object {
        init {
            System.loadLibrary("config")
        }
    }

    private val backThread = HandlerThread("katchy-online", Thread.NORM_PRIORITY)
    private val backHandler: Handler

    private val refreshTask: RefreshAccessTokenTask

    private val id = getKatchyOnlineId()
    private val sec = getKatchyOnlineSec()
    private val refresh = getKatchyOnlineRefresh()

    private var accessToken: String = ""
    private val sessionId = UUID.randomUUID() // Max is up to 36 bytes.

    private val REFRESH_URL = "https://www.googleapis.com/oauth2/v4/token"
    private val BASE_URL = "https://dialogflow.googleapis.com/v2"
    private val GCP_PROJECT = "projects/cloud-sync-service"

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)

        refreshTask = RefreshAccessTokenTask()
        backHandler.post(refreshTask)

    }

    private inner class RefreshAccessTokenTask : Runnable {
        private val CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")
        private val BODY = "refresh_token=$refresh&client_id=$id&client_secret=$sec&grant_type=refresh_token"

        override fun run() {
            val client = OkHttpClient()
            val requestBody = RequestBody.create(CONTENT_TYPE, BODY)
            val request = Request.Builder()
                    .url(REFRESH_URL)
                    .post(requestBody)
                    .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("KatchyOnline.RefreshAccessTokenTask: OK")

                val responseBody: String = response.body().string()

                if (IS_DEBUG) {
                    debugLog("#### RESPONSE")
                    debugLog(responseBody)
                }

                val mapper = jacksonObjectMapper()
                val rootNode = mapper.readTree(responseBody)

                // Parse access token.
                accessToken = rootNode.get("access_token").asText()

                // Parse expiration timeout and register next refresh task.
                if (backThread.isAlive) {
                    val expireSec: Long = rootNode.get("expires_in").asLong()
                    backHandler.postDelayed(this, expireSec * 1000)
                }

            } else {
                if (IS_DEBUG) debugLog("KatchyOnline.RefreshAccessTokenTask: NG")
                errorLog("NG Response = $response")
            }
        }
    }

    override fun getFilterKeywords(): List<String> {
        return listOf() // Empty list.
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

    override fun release() {
        backHandler.removeCallbacks(refreshTask)
        backThread.quitSafely()

    }

    private inner class PostMessageTask(
            val message: String,
            val callback: ConversationStrategy.Callback?,
            val callbackHandler: Handler?) : Runnable {
        private val CONTENT_TYPE: MediaType = MediaType.parse("application/json; charset=utf-8")

        private val countdown = CountDownLatch(1)

        var resMsg: String = ConversationStrategy.SILENT_RESPONSE

        private val GET_URL = "$BASE_URL/$GCP_PROJECT/agent/sessions/$sessionId:detectIntent"

        private fun getJson(message: String): String {
            return """
                {
                    "queryInput":{
                        "text":{
                            "text":"$message",
                            "languageCode":"ja"
                        }
                    },
                    "queryParams":{
                        "timeZone":"Asia/Tokyo"
                    }
                }
            """.trimIndent()
        }

        override fun run() {
            val client = OkHttpClient()
            val requestBody = RequestBody.create(CONTENT_TYPE, getJson(message))
            val request = Request.Builder()
                    .url(GET_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .post(requestBody)
                    .build()

            if (IS_DEBUG) {
                debugLog("Request to Dialogflow of Katchy Online")
                debugLog("URL = $GET_URL")
                debugLog("Header = ${request.headers()}")
                debugLog("Message = $message")
                debugLog("Body = ${getJson(message)}")
            }

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                if (IS_DEBUG) debugLog("KatchyOnline: OK")

                val responseBody: String = response.body().string()

                if (IS_DEBUG) {
                    debugLog("#### RESPONSE")
                    debugLog(responseBody)
                }

                val mapper = jacksonObjectMapper()
                val rootNode = mapper.readTree(responseBody)

                // Parse response.
                resMsg = rootNode.get("queryResult").get("fulfillmentText").asText()
                if (IS_DEBUG) debugLog("Raw response = $resMsg")

            } else {
                errorLog("KatchyOnline: NG")
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
                errorLog("ERROR: Timeout for request to Dialogflow")
            }
        }
    }

    private external fun getKatchyOnlineId(): String
    private external fun getKatchyOnlineSec(): String
    private external fun getKatchyOnlineRefresh(): String
}
