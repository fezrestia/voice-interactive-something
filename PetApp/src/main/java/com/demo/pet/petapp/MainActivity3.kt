@file:Suppress("ConstantConditionIf", "SimplifyBooleanWithConstants", "PrivatePropertyName")

package com.demo.pet.petapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import com.demo.pet.petapp.character.CharacterType
import com.demo.pet.petapp.conversations.ConversationType
import com.demo.pet.petapp.stt.STTType
import com.demo.pet.petapp.tts.OnTtsEngineOptionLoadedCallback
import com.demo.pet.petapp.tts.TTSControllerGoogleCloudApi
import com.demo.pet.petapp.tts.TTSType
import com.demo.pet.petapp.tts.loadTTSEngineOptions
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog

class MainActivity3 : AppCompatActivity() {
    private val IS_DEBUG = Log.IS_DEBUG || false

    private val uiHandler = Handler(Looper.getMainLooper())

    private lateinit var update_gcp_refresh_token_button: Button
    private lateinit var overlay_switch: SwitchCompat
    private lateinit var sound_level_threshold: SeekBar
    private lateinit var stt_engine_selector: Spinner
    private lateinit var tts_engine_selector: Spinner
    private lateinit var conversation_engine_selector: Spinner
    private lateinit var character_model_selector: Spinner
    private lateinit var speak_threshold_indicator: TextView
    private lateinit var tts_engine_option_selector: Spinner

    companion object {
        fun togglePet(isEnabled: Boolean, context: Context) {
            val action: String = if (isEnabled) {
                REQUEST_START_OVERLAY_3
            } else {
                REQUEST_STOP_OVERLAY_3
            }
            val service = Intent(action)
            service.setClass(context , OverlayService3::class.java)
            context.startForegroundService(service)
        }
    }

    private var ttsLabelVsPackage: Map<String, String> = mapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (IS_DEBUG) debugLog("onCreate() : E")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_3)

        update_gcp_refresh_token_button = findViewById(R.id.update_gcp_refresh_token_button)
        overlay_switch = findViewById(R.id.overlay_switch)
        sound_level_threshold = findViewById(R.id.sound_level_threshold)
        stt_engine_selector = findViewById(R.id.stt_engine_selector)
        tts_engine_selector = findViewById(R.id.tts_engine_selector)
        conversation_engine_selector = findViewById(R.id.conversation_engine_selector)
        character_model_selector = findViewById(R.id.character_model_selector)
        speak_threshold_indicator = findViewById(R.id.speak_threshold_indicator)
        tts_engine_option_selector = findViewById(R.id.tts_engine_option_selector)

        // GCP Refresh Token.
        update_gcp_refresh_token_button.setOnClickListener(OnUpdateGcpRefreshTokenButtonClickListener())

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

        // Character model selector.
        val characterAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                CharacterType.values().map { type -> type.toString() } )
        characterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        character_model_selector.adapter = characterAdapter
        character_model_selector.onItemSelectedListener = OnItemSelectedListenerImpl(Constants.KEY_CHARACTER_TYPE)


        // Prepare.
        TTSControllerGoogleCloudApi.onStaticCreate(this)

        if (IS_DEBUG) debugLog("onCreate() : X")
    }

    private inner class OnUpdateGcpRefreshTokenButtonClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            if (IS_DEBUG) debugLog("Update GCP Refresh Token")

            TTSControllerGoogleCloudApi.updateRefreshToken()
        }
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

            when (spKey) {
                Constants.KEY_TTS_TYPE -> {
                    val ttsType = TTSType.valueOf(selected)
                    updateTtsSubOption(ttsType)
                }

                Constants.KEY_TTS_TYPE_OPTION_LABEL -> {
                    // Also update TTS package.
                    val pkg = ttsLabelVsPackage[selected]
                    PetApplication.getSP().edit().putString(
                            Constants.KEY_TTS_TYPE_OPTION_PACKAGE,
                            pkg)
                            .apply()
                }
            }

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


        val data = intent.data
        if (IS_DEBUG) {
            debugLog("## intent.data = ${data.toString()}")
            debugLog("## intent = ${intent.toString()}")
        }

        val codeRegExp: Regex = "(?<=code=)[^&]+".toRegex()
        val matched: MatchResult? = codeRegExp.find(intent.data.toString())

        if (matched != null) {
            if (IS_DEBUG) debugLog("### matched.value = ${matched.value}")

            // Prepare.
            TTSControllerGoogleCloudApi.onStaticResume(matched.value)

        } else {
            if (IS_DEBUG) debugLog("Can not match code in response, this is normal launch.")
        }


        loadConfigs()

        // Speak level threshold.
        val curThreshold = PetApplication.getSP().getInt(
                Constants.KEY_SPEAK_THRESHOLD,
                Constants.SPEAK_THRESHOLD_DEFAULT)
        sound_level_threshold.progress = curThreshold - Constants.SPEAK_THRESHOLD_MIN

        if (IS_DEBUG) debugLog("onResume() : X")
    }

    private fun loadConfigs() {
        // TTS.
        val defaultTtsType = TTSType.ANDROID
        val tts = PetApplication.getSP().getString(
                Constants.KEY_TTS_TYPE,
                defaultTtsType.toString()) as String
        val ttsType = TTSType.valueOf(tts)
        tts_engine_selector.setSelection(ttsType.ordinal)
        // After then, onItemSelected callback will be invoked.
        // So, do NOT call updateTtsSubOption() here.

        // STT.
        val defaultSttType = STTType.GOOGLE_CLOUD_PLATFORM
        val stt = PetApplication.getSP().getString(
                Constants.KEY_STT_TYPE,
                defaultSttType.toString()) as String
        stt_engine_selector.setSelection(STTType.valueOf(stt).ordinal)

        // Conversation.
        val defaultConversationType = ConversationType.USER_DEF
        val conversation = PetApplication.getSP().getString(
                Constants.KEY_CONVERSATION_TYPE,
                defaultConversationType.toString()) as String
        conversation_engine_selector.setSelection(ConversationType.valueOf(conversation).ordinal)

        // Current state.
        overlay_switch.isChecked = PetApplication.isKatchy3Active

    }

    private fun updateTtsSubOption(ttsType: TTSType) {
        loadTTSEngineOptions(
                this,
                ttsType,
                object : OnTtsEngineOptionLoadedCallback {
                    override fun onLoaded(labelVsPackage: Map<String, String>) {
                        ttsLabelVsPackage = labelVsPackage

                        uiHandler.post {
                            val options = ttsLabelVsPackage.keys.sorted()

                            val ttsOptionAdapter = ArrayAdapter(
                                    this@MainActivity3,
                                    android.R.layout.simple_spinner_item,
                                    options)

                            ttsOptionAdapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item)
                            tts_engine_option_selector.adapter = ttsOptionAdapter
                            tts_engine_option_selector.onItemSelectedListener =
                                    OnItemSelectedListenerImpl(Constants.KEY_TTS_TYPE_OPTION_LABEL)

                            // Update selected state.
                            val storedOption = PetApplication.getSP().getString(
                                    Constants.KEY_TTS_TYPE_OPTION_LABEL,
                                    Constants.VAL_DEFAULT) as String
                            val idx = options.indexOf(storedOption)
                            if (idx >= 0) {
                                // Stored option is available.
                                tts_engine_option_selector.setSelection(idx)
                            } else {
                                // Stored option is NOT available. Reset to default.
                                PetApplication.getSP().edit().putString(
                                        Constants.KEY_TTS_TYPE_OPTION_LABEL,
                                        Constants.VAL_DEFAULT)
                                        .apply()
                                PetApplication.getSP().edit().putString(
                                        Constants.KEY_TTS_TYPE_OPTION_PACKAGE,
                                        Constants.VAL_DEFAULT)
                                        .apply()
                            }
                        }
                    }
                } )
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

    private val requestCodeManagePermissions = 200

    private val isSystemAlertWindowPermissionGranted: Boolean
        get() = Settings.canDrawOverlays(this)

    private val isCameraPermissionGranted: Boolean
        get() = (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)

    private val isMicPermissionGranted: Boolean
        get() = (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED)

    private val startOverlayPermissionRequest = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
        if (Log.IS_DEBUG) debugLog("overlay permission result = $result")
        if (!isSystemAlertWindowPermissionGranted) {
            if (Log.IS_DEBUG) debugLog("  Overlay permission is not granted yet.")
            finish()
        }
    }

    /**
     * Check permission.
     *
     * @return immediateReturnRequired
     */
    private fun checkMandatoryPermissions(): Boolean {
        if (Log.IS_DEBUG) debugLog("checkMandatoryPermissions()")

        if (!isSystemAlertWindowPermissionGranted) {
            // Start permission setting.
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))

            startOverlayPermissionRequest.launch(intent)

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
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (Log.IS_DEBUG) debugLog("onRequestPermissionsResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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
