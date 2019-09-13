@file:Suppress("PrivatePropertyName", "ConstantConditionIf")

package com.demo.pet.petapp.stt

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils

import com.google.auth.Credentials
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.okhttp.OkHttpChannelProvider
import io.grpc.stub.StreamObserver

import com.demo.pet.petapp.util.errorLog
import com.demo.pet.petapp.R
import com.demo.pet.petapp.util.debugLog
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechGrpc
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import kotlin.math.max

/**
 * Google Cloud Platform natural language API client.
 */
class GoogleSpeechApi(val context: Context) {
    private val IS_DEBUG = false // Log.IS_DEBUG

    private val mainHandler = Handler(context.mainLooper)
    private val backThread = HandlerThread("back-worker")
    private val backHandler: Handler

    private var accessToken: AccessToken? = null

    private var api: SpeechGrpc.SpeechStub? = null

    interface Callback {
        fun onSpeechRecognized(text: String, isEnd: Boolean)
    }

    var callback: Callback? = null

    private var requestObserver: StreamObserver<StreamingRecognizeRequest>? = null
    private val responseObserver = object: StreamObserver<StreamingRecognizeResponse> {
        override fun onNext(response: StreamingRecognizeResponse) {
            if (IS_DEBUG) debugLog("ResponseObserver.onNext()")

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

        override fun onError(err: Throwable?) {
            if (IS_DEBUG) errorLog("ResponseObserver.onError() : $err")
        }

        override fun onCompleted() {
            if (IS_DEBUG) debugLog("ResponseObserver.onCompleted()")
        }
    }

    init {
        backThread.start()
        backHandler = Handler(backThread.looper)

        // Start setup Web API client.
        backHandler.post(SetupWebApiTask())

    }

    /**
     * Life-Cycle
     */
    fun release() {
        // Release Web API client.
        backHandler.post(ReleaseWebApiTask())
        backThread.quit()
    }

    /**
     * Start recognition and wait for audio frame.
     */
    fun startRecog(samplingRate: Int) {
        val curApi = api ?: return

        requestObserver = curApi.streamingRecognize(responseObserver)
        requestObserver?.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(samplingRate)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build())
                .build())
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
        requestObserver?.onCompleted()
        requestObserver = null
    }

    /**
     * Input recorded each audio frame.
     *
     * @param data Audio frame.
     * @param size Data size.
     */
    fun recognize(data: ByteArray, size: Int) {
        // Call web API.
        requestObserver?.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build())
    }

    private inner class SetupWebApiTask : Runnable {
        private val prefName = "gcp-token-prefs"
        private val prefKeyToken = "key-token"
        private val prefKeyExpirationTime = "key-expiration-time"

        private val ACCESS_TOKEN_EXPIRATION_TIMEOUT_MILLIS: Long = 30 * 60 * 1000 // 30 min
        private val ACCESS_TOKEN_REFRESH_MARGIN_MILLIS: Long = 60 * 1000 // 1 min

        private val SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform")

        private val HOST = "speech.googleapis.com"
        private val PORT = 443

        override fun run() {
            val sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

            // Current access token.
            val curToken = getCurrentAccessToken(sp)
            if (curToken != null) {
                accessToken = curToken
            }

            if (accessToken == null) {
                // Request new access token.
                val newToken = requestNewAccessToken(sp)
                if (newToken != null) {
                    accessToken = newToken
                }
            }

            val token: AccessToken = accessToken ?: throw RuntimeException("Failed to get AccessToken.")

            // Gen API.
            api = getWebApi(token)

            // Schedule next AccessToken refresh.
            val interval = max(
                    token.expirationTime.time - System.currentTimeMillis() - ACCESS_TOKEN_REFRESH_MARGIN_MILLIS,
                    ACCESS_TOKEN_EXPIRATION_TIMEOUT_MILLIS)
            backHandler.postDelayed(SetupWebApiTask(), interval)
        }

        private fun getCurrentAccessToken(sp: SharedPreferences): AccessToken? {
            val token = sp.getString(prefKeyToken, null)
            val expirationTime = sp.getLong(prefKeyExpirationTime, 0L)

            // Check current token is valid or not.
            if (token != null && expirationTime != 0L) {
                // Already exists. Check timeout.
                if (expirationTime
                        > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIMEOUT_MILLIS) {
                    // Valid token.
                    return AccessToken(token, Date(expirationTime))
                }
            }

            return null
        }

        private fun requestNewAccessToken(sp: SharedPreferences): AccessToken? {
            val inputStream = context.resources.openRawResource(R.raw.credential)

            try {
                val credentials = GoogleCredentials
                        .fromStream(inputStream)
                        .createScoped(SCOPE)

                val newToken = credentials.refreshAccessToken()

                sp.edit()
                        .putString(prefKeyToken, newToken.tokenValue)
                        .putLong(prefKeyExpirationTime, newToken.expirationTime.time)
                        .apply()

                return newToken

            } catch (e: IOException) {
                e.printStackTrace()
                errorLog("Failed to get AccessToken.")
                return null
            }
        }

        private fun getWebApi(token: AccessToken): SpeechGrpc.SpeechStub {
            val channel = OkHttpChannelProvider()
                    .builderForAddress(HOST, PORT)
                    .nameResolverFactory(DnsNameResolverProvider())
                    .intercept(ClientInterceptorImpl(
                            GoogleCredentials(token).createScoped(SCOPE)))
                    .build()

            return SpeechGrpc.newStub(channel)
        }

        private inner class ClientInterceptorImpl(val credentials: Credentials) : ClientInterceptor {
            var cachedMetadata: Metadata? = null
            var lastMetadata: Map<String, List<String>>? = null

            override fun <ReqT, RespT> interceptCall(
                    method: MethodDescriptor<ReqT, RespT>,
                    callOptions: CallOptions,
                    next: Channel): ClientCall<ReqT, RespT> {

                return object: ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOptions)) {

                    @Throws(StatusException::class)
                    override fun checkedStart(
                            responseListener: Listener<RespT>,
                            headers: Metadata) {
                        val uri = serviceUri(next, method)

                        synchronized(this) {
                            val latestMetadata = getRequestMetadata(uri)
                            if (lastMetadata == null || lastMetadata != latestMetadata) {
                                lastMetadata = latestMetadata
                                cachedMetadata = toHeaders(lastMetadata)
                            }
                            val cached = cachedMetadata
                            if (cached != null) {
                                headers.merge(cached)
                            }
                        }

                        delegate().start(responseListener, headers)
                    }
                }
            }

            @Throws(StatusException::class)
            private fun serviceUri(channel: Channel, method: MethodDescriptor<*, *>): URI {
                val authority = channel.authority() ?: throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException()
                // Always use HTTPS, by definition.
                val scheme = "https"
                val defaultPort = PORT
                val path = "/" + MethodDescriptor.extractFullServiceName(method.fullMethodName)
                try {
                    var uri = URI(scheme, authority, path, null, null)

                    // The default port must not be present. Alternative ports should be present.
                    if (uri.port == defaultPort) {
                        uri = removePort(uri)
                    }
                    return uri

                } catch (e: URISyntaxException) {
                    throw Status.UNAUTHENTICATED
                            .withDescription("Unable to construct service URI for auth")
                            .withCause(e).asException()
                }
            }

            @Throws(StatusException::class)
            private fun removePort(uri: URI): URI {
                try {
                    return URI(
                            uri.scheme,
                            uri.userInfo,
                            uri.host,
                            -1, // Port
                            uri.path,
                            uri.query,
                            uri.fragment)
                } catch (e: URISyntaxException) {
                    throw Status.UNAUTHENTICATED
                            .withDescription("Unable to construct service URI after removing port")
                            .withCause(e).asException()
                }
            }

            @Throws(StatusException::class)
            private fun getRequestMetadata(uri: URI): Map<String, List<String>> {
                try {
                    return credentials.getRequestMetadata(uri)
                } catch (e: IOException) {
                    throw Status.UNAUTHENTICATED.withCause(e).asException()
                }

            }

            private fun toHeaders(metadata: Map<String, List<String>>?): Metadata {
                val headers = Metadata()

                if (metadata != null) {
                    for (key in metadata.keys) {
                        val headerKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                        val values = metadata[key]
                        if (values != null) {
                            for (value in values) {
                                headers.put(headerKey, value)
                            }
                        }
                    }
                }

                return headers
            }
        }
    }

    private inner class ReleaseWebApiTask : Runnable {
        override fun run() {
            val curApi: SpeechGrpc.SpeechStub = api ?: return

            val channel = curApi.channel as ManagedChannel?
            if (channel != null && !channel.isShutdown) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            api = null
        }
    }

}
