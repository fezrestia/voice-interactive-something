package com.demo.pet.petapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.demo.pet.petapp.tts.TTSControllerGoogleCloudApi
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Log.IS_DEBUG) debugLog("BootCompletedReceiver.onReceive() : E")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Refresh Google Cloud API access token here. (once a half-year is necessary)
            val tts = TTSControllerGoogleCloudApi(context, Constants.VAL_DEFAULT)
            tts.refreshAccessToken()
            tts.release()
        }

        if (Log.IS_DEBUG) debugLog("BootCompletedReceiver.onReceive() : X")
    }
}