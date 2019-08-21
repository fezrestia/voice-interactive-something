@file:Suppress(
        "ConstantConditionIf",
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants")

package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import com.demo.pet.petapp.stt.STTType
import com.demo.pet.petapp.tts.TTSController
import com.demo.pet.petapp.tts.TTSControllerAndroid
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val IS_DEBUG = false || Log.IS_DEBUG

    private var ttsCtrl: TTSController? = null

    private lateinit var soundPool: SoundPool
    private var soundWan: Int = 0
    private var soundKuun: Int = 0

    private val DEFAULT_ENGINE = "default"

    companion object {
        fun togglePet(isEnabled: Boolean, context:Context) {
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
        if (IS_DEBUG) debugLog("onCreate()")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        overlay_switch.isChecked = OverlayRootView.isActive()
        overlay_switch.setOnCheckedChangeListener(OnCheckedChangeListenerImpl())

        speak_input_text.setOnClickListener { speakInputText() }

        setupSTTEngineSelector()
    }

    private fun setupSTTEngineSelector() {
        val spinner: Spinner = findViewById(R.id.stt_eigine_selector)
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                PetApplication.getSP().edit().putString(
                        Constants.KEY_STT_TYPE,
                        STTType.POCKET_SPHINX.toString())
                        .apply()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (parent?.selectedItem as String) {
                    "Android" -> {
                        PetApplication.getSP().edit().putString(
                                Constants.KEY_STT_TYPE,
                                STTType.ANDROID_SPEECH_RECOGNIZER.toString())
                                .apply()

                    }
                    "PocketSphinx" -> {
                        PetApplication.getSP().edit().putString(
                                Constants.KEY_STT_TYPE,
                                STTType.POCKET_SPHINX.toString())
                                .apply()
                    }
                    "GoogleWebApi" -> {
                        PetApplication.getSP().edit().putString(
                                Constants.KEY_STT_TYPE,
                                STTType.GOOGLE_WEB_API.toString())
                                .apply()
                    }
                    else -> {
                        PetApplication.getSP().edit().putString(
                                Constants.KEY_STT_TYPE,
                                STTType.POCKET_SPHINX.toString())
                                .apply()
                    }
                }
            }
        }
    }

    private inner class OnCheckedChangeListenerImpl : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (IS_DEBUG) debugLog("Overlay switch changed to : $isChecked")

            togglePet(isChecked, this@MainActivity)

            if (isChecked) {
                finish()
            }
        }
    }

    override fun onResume() {
        if (IS_DEBUG) debugLog("onResume()")
        super.onResume()

        // Mandatory permission check.
        if (isFinishing) {
            return
        }
        if (checkMandatoryPermissions()) {
            return
        }

        // TTS.
        ttsCtrl = TTSControllerAndroid(this, Constants.VAL_DEFAULT)

        prepareButtons()

        prepareSoundPool()

    }

    private fun prepareSoundPool() {
        // Sound.
        val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttr)
                .setMaxStreams(2)
                .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, _ ->
            if (IS_DEBUG) debugLog("SoundPool.onLoadComplete() : ID=$sampleId")
        }

        soundWan = soundPool.load(this, R.raw.wan_wan, 1)
        soundKuun = soundPool.load(this, R.raw.wan_kuun, 1)
    }

    private fun prepareButtons() {
        // Engine selector.
        engine_selector.setOnCheckedChangeListener { _, _ ->
            ttsCtrl = TTSControllerAndroid(this@MainActivity, Constants.VAL_DEFAULT)
        }
        engine_selector.removeAllViews()
        val radioButton = RadioButton(this)
        radioButton.text = DEFAULT_ENGINE
        engine_selector.addView(radioButton)

        // Speak buttons.
        key_q.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_q)) }
        key_w.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_w)) }
        key_e.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_e)) }
        key_r.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_r)) }
        key_t.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_t)) }

        key_a.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_a)) }
        key_s.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_s)) }
        key_d.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_d)) }
        key_f.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_f)) }
        key_g.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_g)) }
        key_h.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_h)) }
        key_j.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_j)) }
        key_k.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_k)) }
        key_l.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_l)) }

        key_z.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_z)) }

        key_1.setOnClickListener { soundPool.play(soundWan, 1.0f, 1.0f, 0, 0, 1.0f) }
        key_2.setOnClickListener { soundPool.play(soundKuun, 1.0f, 1.0f, 0, 0, 1.0f) }

        key_5.setOnClickListener { ttsCtrl?.speak(getString(R.string.key_5)) }

    }

    private fun speakInputText() {
        val text = input_text.text.toString()

        if (IS_DEBUG) debugLog("speakInputText() : text = $text")

        ttsCtrl?.speak(text)
    }

    override fun onPause() {
        if (IS_DEBUG) debugLog("onPause()")
        super.onPause()

        ttsCtrl?.release()
        ttsCtrl = null

        soundPool.release()
    }

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
