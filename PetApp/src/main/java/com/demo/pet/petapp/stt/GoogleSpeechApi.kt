@file:Suppress("PrivatePropertyName", "ConstantConditionIf", "SimplifyBooleanWithConstants")

package com.demo.pet.petapp.stt

import java.util.Locale

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils

import com.google.auth.oauth2.GoogleCredentials

import com.demo.pet.petapp.R
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog
import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString

/**
 * Google Cloud Platform natural language API client.
 */
class GoogleSpeechApi(val context: Context) {
    private val IS_DEBUG = false || Log.IS_DEBUG

    private val mainHandler = Handler(context.mainLooper)
    private val backThread = HandlerThread("back-worker")
    private val backHandler: Handler

    private var speechClient: SpeechClient? = null
    private var requestStream: ClientStream<StreamingRecognizeRequest>? = null
    private var streamingConfig: StreamingRecognitionConfig? = null

    private var is1stRequest = true

    interface Callback {
        fun onSpeechRecognized(text: String, isEnd: Boolean)
    }

    var callback: Callback? = null

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)
        backHandler.post(SetupWebApiTask())
    }

    /**
     * Life-Cycle
     */
    fun release() {
        backHandler.post(ReleaseWebApiTask())
        backThread.quit()
    }

    /**
     * Start recognition and wait for audio frame.
     */
    fun startRecog(samplingRate: Int) {
        speechClient?.let {
            requestStream = it.streamingRecognizeCallable()
                    .splitCall(object : ResponseObserver<StreamingRecognizeResponse> {
                        override fun onStart(controller: StreamController?) {
                            if (IS_DEBUG) debugLog("ResponseObserver.onStart()")
                            // NOP.
                        }

                        override fun onResponse(response: StreamingRecognizeResponse) {
                            if (IS_DEBUG) debugLog("ResponseObserver.onResponse()")

                            var text: String? = null
                            var isFinal = false

                            if (response.resultsCount > 0) {
                                val result = response.getResults(0)
                                isFinal = result.isFinal
                                if (result.alternativesCount > 0) {
                                    val alternative = result.getAlternatives(0)
                                    text = alternative.transcript
                                }
                            }

                            if (text != null) {
                                if (IS_DEBUG) debugLog("Speech Recognized = $text")

                                mainHandler.post { callback?.onSpeechRecognized(text, isFinal) }
                            }
                        }

                        override fun onError(t: Throwable) {
                            errorLog("ResponseObserver.onError() : ${t.message}")
                            // NOP.
                        }

                        override fun onComplete() {
                            if (IS_DEBUG) debugLog("ResponseObserver.onComplete()")
                            // NOP.
                        }
                    } )

            streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(RecognitionConfig.newBuilder()
                            .setLanguageCode(getDefaultLanguageCode())
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setSampleRateHertz(samplingRate)
                            .build())
                    .setInterimResults(true)
                    .setSingleUtterance(true)
                    .build()

            is1stRequest = true
        }

    }

    private fun getDefaultLanguageCode(): String {
        val locale = Locale.getDefault()
        val language = StringBuilder(locale.language)
        val country = locale.country

        if (!TextUtils.isEmpty(country)) {
            language.append("-")
            language.append(country)
        }

        if (IS_DEBUG) debugLog("## Current Language = $language")

        return language.toString()
    }

    /**
     * Stop recognition.
     */
    fun stopRecog() {
        streamingConfig = null

        requestStream?.closeSend()
        requestStream = null
    }

    /**
     * Input recorded each audio frame.
     *
     * @param data Audio frame.
     * @param size Data size.
     */
    fun recognize(data: ByteArray, size: Int) {
        requestStream?.let {
            val builder = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(data, 0, size))

            if (is1stRequest) {
                builder.streamingConfig = streamingConfig
                is1stRequest = false
            }

            it.send(builder.build())
        }
    }

    private inner class SetupWebApiTask : Runnable {
        override fun run() {
            val cred = context.resources.openRawResource(R.raw.credential)

            speechClient = SpeechClient.create(
                    SpeechSettings.newBuilder()
                            .setCredentialsProvider { GoogleCredentials.fromStream(cred) }
                            .build())
        }
    }

    private inner class ReleaseWebApiTask : Runnable {
        override fun run() {
            stopRecog()

            speechClient?.shutdownNow()
            speechClient = null

        }
    }

}
