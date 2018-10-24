@file:Suppress("ConstantConditionIf")

package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import com.demo.pet.petapp.stt.STTType
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity2 : AppCompatActivity() {

    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = Log.IS_DEBUG

    companion object {
        fun togglePet(isEnabled: Boolean, context: Context) {
            val action: String = if (isEnabled) {
                REQUEST_START_OVERLAY
            } else {
                REQUEST_STOP_OVERLAY
            }
            val service = Intent(action)
            service.setClass(context , OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (IS_DEBUG) debugLog("onCreate() : E")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_2)

        overlay_switch.isChecked = OverlayRootView.isActive()
        overlay_switch.setOnCheckedChangeListener(OnCheckedChangeListenerImpl())

        if (IS_DEBUG) debugLog("onCreate() : X")
    }

    private inner class OnCheckedChangeListenerImpl : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (IS_DEBUG) debugLog("Overlay switch changed to : $isChecked")

            togglePet(isChecked, this@MainActivity2)
        }
    }

    override fun onResume() {
        if (IS_DEBUG) debugLog("onResume() : E")
        super.onResume()

        // Mandatory permission check.
        if (isFinishing) {
            return
        }
        if (checkMandatoryPermissions()) {
            return
        }

        // TTS fixed.
        PetApplication.getSP().edit().putString(
                Constants.KEY_TTS_TYPE,
                TTSType.ANDROID.toString())
                .apply()

        // STT fixed.
        PetApplication.getSP().edit().putString(
                Constants.KEY_STT_TYPE,
                STTType.GOOGLE_WEB_API.toString())
                .apply()

        if (IS_DEBUG) debugLog("onResume() : X")
    }

    override fun onPause() {
        if (IS_DEBUG) debugLog("onPause() : E")



        super.onPause()
        if (IS_DEBUG) debugLog("onPause() : X")
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("onDestroy() : E")



        super.onDestroy()
        if (IS_DEBUG) debugLog("onDestroy() : X")
    }



    //// RUNTIME PERMISSION SUPPORT.

    @Suppress("PrivatePropertyName")
    private val REQ_CODE_OVERLAY_PERMISSION: Int = 1000

    @SuppressLint("ObsoleteSdkInt")
    private fun isSystemAlertWindowPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    @SuppressLint("InlinedApi", "ObsoleteSdkInt")
    private fun checkMandatoryPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !isSystemAlertWindowPermissionGranted()) {
            // Start permission setting.
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQ_CODE_OVERLAY_PERMISSION) {
            if (!isSystemAlertWindowPermissionGranted()) {
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

}




