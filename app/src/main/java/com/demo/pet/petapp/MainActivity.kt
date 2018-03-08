package com.demo.pet.petapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        debugLog("onCreate()")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        overlay_switch.isChecked = OverlayRootView.isActive()
        overlay_switch.setOnCheckedChangeListener { _: CompoundButton, is_checked: Boolean ->
            debugLog("Overlay switch changed to : " + is_checked)

            val action: String = if (is_checked) {
                // Enabled.
                REQUEST_START_OVERLAY
            } else {
                // Disabled.
                REQUEST_STOP_OVERLAY
            }
            val service = Intent(action)
            service.setClass(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service)
            } else {
                startService(service)
            }
        }



        // Example.
        stringFromJNI()
    }

    override fun onResume() {
        debugLog("onResume()")
        super.onResume()

        // Mandatory permission check.
        if (isFinishing) {
            return
        }
        if (checkMandatoryPermissions()) {
            return
        }
    }

    override fun onPause() {
        debugLog("onPause()")
        super.onPause()


    }

    private val REQ_CODE_OVERLAY_PERMISSION: Int = 1000

    private fun isSystemAlertWindowPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun checkMandatoryPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isSystemAlertWindowPermissionGranted()) {
                // Start permission setting.
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName))
                startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
                return true
            }
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





    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
