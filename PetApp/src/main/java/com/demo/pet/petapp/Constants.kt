@file:Suppress("MayBeConstant")

package com.demo.pet.petapp

import okhttp3.MediaType.Companion.toMediaTypeOrNull

object Constants {
    val KEY_VERSION = "version"
    val VAL_INVALID_VERSION = -1
    val VAL_VERSION = 1

    val KEY_STT_TYPE = "key-stt-type"
    val KEY_TTS_TYPE = "key-tts-type"
    val KEY_TTS_TYPE_OPTION_LABEL = "key-tts-type-option-label"
    val KEY_TTS_TYPE_OPTION_PACKAGE = "key-tts-type-option-package"
    val KEY_CONVERSATION_TYPE = "key-conversation-type"
    val KEY_CHARACTER_TYPE = "key-character-type"

    val KEY_KEYWORD_PROTOCOLS = "key-keyword-protocols"

    val KEY_SPEAK_THRESHOLD = "key-speak-threshold"

    val SPEAK_THRESHOLD_MIN: Int = 0
    val SPEAK_THRESHOLD_MAX: Int = 1500
    val SPEAK_THRESHOLD_DEFAULT: Int = 1000

    val VAL_DEFAULT = "default"

    val KEY_GCP_REFRESH_TOKEN = "key-gcp-refresh-token"


    // Google Cloud Platform constants.

    val GCP_CONTENT_TYPE = "application/x-www-form-urlencoded".toMediaTypeOrNull()

    val GCP_CODE_VERIFIER = "randomcodeverifierstringforgooglecloudplatformapiauthentication"
    val GCP_CODE_CHALLENGE_METHOD = "plain"

//    val GCP_REDIRECT_URI = "com.demo.pet.petapp.debug:oauth2redirect" // debug
    val GCP_REDIRECT_URI = "com.demo.pet.petapp:oauth2redirect" // release

    val GCP_SCOPE = "https://www.googleapis.com/auth/cloud-platform"



}
