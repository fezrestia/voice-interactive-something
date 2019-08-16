@file:Suppress("ConstantConditionIf", "SimplifyBooleanWithConstants")

package com.demo.pet.petapp

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import com.demo.pet.petapp.conversations.ConversationType
import com.demo.pet.petapp.stt.STTType
import com.demo.pet.petapp.tts.TTSType
import kotlinx.android.synthetic.main.activity_main_3.*

class MainActivity3 : AppCompatActivity() {
    @Suppress("PrivatePropertyName")
    private val IS_DEBUG = Log.IS_DEBUG || false

    companion object {
        fun togglePet(isEnabled: Boolean, context: Context) {
            val action: String = if (isEnabled) {
                REQUEST_START_OVERLAY_3
            } else {
                REQUEST_STOP_OVERLAY_3
            }
            val service = Intent(action)
            service.setClass(context , OverlayService3::class.java)
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

        setContentView(R.layout.activity_main_3)

        // En/Disable switch.
        overlay_switch.setOnCheckedChangeListener(OnCheckedChangeListenerImpl())

        // Sound level threshold.
        sound_level_threshold.max = Constants.SPEAK_THRESHOLD_MAX - Constants.SPEAK_THRESHOLD_MIN
        sound_level_threshold.setOnSeekBarChangeListener(OnSeekBarChangeListenerImpl())

        // STT Engine selector.
        val sttAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                STTType.values().map { type -> type.toString() } )
        sttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stt_engine_selector.adapter = sttAdapter
        stt_engine_selector.onItemSelectedListener = OnItemSelectedListenerImpl(Constants.KEY_STT_TYPE)

        // TTS Engine selector.
        val ttsAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                TTSType.values().map { type -> type.toString() } )
        ttsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tts_engine_selector.adapter = ttsAdapter
        tts_engine_selector.onItemSelectedListener = OnItemSelectedListenerImpl(Constants.KEY_TTS_TYPE)

        // Conversation Engine selector.
        val conversationAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                ConversationType.values().map { type -> type.toString() } )
        conversationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        conversation_engine_selector.adapter = conversationAdapter
        conversation_engine_selector.onItemSelectedListener = OnItemSelectedListenerImpl(Constants.KEY_CONVERSATION_TYPE)

        if (IS_DEBUG) debugLog("onCreate() : X")
    }

    private inner class OnCheckedChangeListenerImpl : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (IS_DEBUG) debugLog("Overlay switch changed to : $isChecked")

            togglePet(isChecked, this@MainActivity3)
        }
    }

    private inner class OnSeekBarChangeListenerImpl : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, isFromUser: Boolean) {
            val cur = progress + Constants.SPEAK_THRESHOLD_MIN
            speak_threshold_indicator.text = cur.toString()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            // NOP.
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            val cur: Int = seekBar.progress + Constants.SPEAK_THRESHOLD_MIN
            PetApplication.getSP().edit().putInt(Constants.KEY_SPEAK_THRESHOLD, cur).apply()
        }
    }

    private inner class OnItemSelectedListenerImpl(val spKey: String)
            : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val spinner = parent as Spinner
            val selected: String = spinner.selectedItem as String

            PetApplication.getSP().edit().putString(spKey, selected).apply()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // NOP.
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

        loadConfigs()

        // Speak level threshold.
        val curThreshold = PetApplication.getSP().getInt(
                Constants.KEY_SPEAK_THRESHOLD,
                Constants.SPEAK_THRESHOLD_DEFAULT)
        sound_level_threshold.progress = curThreshold - Constants.SPEAK_THRESHOLD_MIN

        // Immersive mode.
        root_view.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE)

        if (IS_DEBUG) debugLog("onResume() : X")
    }

    private fun loadConfigs() {
        // TTS.
        val tts = PetApplication.getSP().getString(
                Constants.KEY_TTS_TYPE,
                TTSType.ANDROID.toString()) as String
        tts_engine_selector.setSelection(TTSType.valueOf(tts).ordinal)

        // STT.
        val stt = PetApplication.getSP().getString(
                Constants.KEY_STT_TYPE,
                STTType.GOOGLE_WEB_API.toString()) as String
        stt_engine_selector.setSelection(STTType.valueOf(stt).ordinal)

        // Conversation.
        val conversation = PetApplication.getSP().getString(
                Constants.KEY_CONVERSATION_TYPE,
                ConversationType.USER_DEF.toString()) as String
        conversation_engine_selector.setSelection(ConversationType.valueOf(conversation).ordinal)

        // Current state.
        overlay_switch.isChecked = PetApplication.isKatchy3Active

    }

    override fun onPause() {
        if (IS_DEBUG) debugLog("onPause() : E")

        // NOP.

        super.onPause()
        if (IS_DEBUG) debugLog("onPause() : X")
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("onDestroy() : E")

        // NOP.

        super.onDestroy()
        if (IS_DEBUG) debugLog("onDestroy() : X")
    }

    //// RUNTIME PERMISSION SUPPORT.

    private val requestCodeManageOverlayPermission = 100
    private val requestCodeManagePermissions = 200

    private val isRuntimePermissionRequired: Boolean
        get() = Build.VERSION_CODES.M <= Build.VERSION.SDK_INT

    private val isSystemAlertWindowPermissionGranted: Boolean
        @TargetApi(Build.VERSION_CODES.M)
        get() = Settings.canDrawOverlays(this)

    private val isCameraPermissionGranted: Boolean
        @TargetApi(Build.VERSION_CODES.M)
        get() = (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)

    private val isMicPermissionGranted: Boolean
        @TargetApi(Build.VERSION_CODES.M)
        get() = (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED)

    /**
     * Check permission.
     *
     * @return immediateReturnRequired
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun checkMandatoryPermissions(): Boolean {
        if (Log.IS_DEBUG) debugLog("checkMandatoryPermissions()")

        if (isRuntimePermissionRequired) {
            if (!isSystemAlertWindowPermissionGranted) {
                // Start permission setting.
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))

                try {
                    startActivityForResult(intent, requestCodeManageOverlayPermission)
                } catch (e: RuntimeException) {
                    errorLog("startActivityForResult ERR : ${e.printStackTrace()}")
                    return false
                }

                return true
            }

            val permissions = mutableListOf<String>()

            if (!isCameraPermissionGranted) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (!isMicPermissionGranted) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }

            return if (permissions.isNotEmpty()) {
                requestPermissions(
                        permissions.toTypedArray(),
                        requestCodeManagePermissions)
                true
            } else {
                false
            }
        } else {
            return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (Log.IS_DEBUG) debugLog("onActivityResult()")

        if (requestCode == requestCodeManageOverlayPermission) {
            if (!isSystemAlertWindowPermissionGranted) {
                if (Log.IS_DEBUG) debugLog("  Overlay permission is not granted yet.")
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (Log.IS_DEBUG) debugLog("onRequestPermissionsResult()")

        if (requestCode == requestCodeManagePermissions) {
            if (!isCameraPermissionGranted) {
                if (Log.IS_DEBUG) debugLog("  Camera permission is not granted yet.")
                finish()
            }
            if (!isMicPermissionGranted) {
                if (Log.IS_DEBUG) debugLog("  Mic permission is not granted yet.")
                finish()
            }
        }
    }
}
