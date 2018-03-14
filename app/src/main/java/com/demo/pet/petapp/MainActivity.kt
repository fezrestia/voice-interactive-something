package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var ttsCtrl: TTSController? = null
    private lateinit var installedEngines: List<TextToSpeech.EngineInfo>

    private lateinit var soundPool: SoundPool
    private var soundWan: Int = 0
    private var soundKuun: Int = 0

    companion object {
        init {
            System.loadLibrary("native-lib")
        }

        var userTtsEngine: String = TTSController.DEFAULT_ENGINE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debugLog("onCreate()")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        overlay_switch.isChecked = OverlayRootView.isActive()
        overlay_switch.setOnCheckedChangeListener(OnCheckedChangeListenerImpl())

        speak_input_text.setOnClickListener( { speakInputText() } )

        // Supported engines.
        installedEngines = TTSController.getSupportedEngines(this)
        installedEngines.forEach {
            debugLog("Installed Engine = ${it.label}")
        }
    }

    private inner class OnCheckedChangeListenerImpl : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            debugLog("Overlay switch changed to : $isChecked")

            val action: String = if (isChecked) {
                // Enabled.
                REQUEST_START_OVERLAY
            } else {
                // Disabled.
                REQUEST_STOP_OVERLAY
            }
            val service = Intent(action)
            service.setClass(this@MainActivity, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service)
            } else {
                startService(service)
            }
        }
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

        // TTS.
        ttsCtrl = TTSController(this)

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
        soundPool.setOnLoadCompleteListener(SoundPool.OnLoadCompleteListener() { soundPool, sampleId, status ->
            debugLog("SoundPool.onLoadComplete() : ID=$sampleId")
        } )

        soundWan = soundPool.load(this, R.raw.wan_wan, 1)
        soundKuun = soundPool.load(this, R.raw.wan_kuun, 1)
    }

    private fun prepareButtons() {
        // Engine selector.
        engine_selector.setOnCheckedChangeListener(
                { _: RadioGroup, selectedId: Int ->
                    userTtsEngine = when (selectedId) {
                        -1 -> {
                            TTSController.DEFAULT_ENGINE
                        }
                        else -> {
                            val radioButton: RadioButton = this@MainActivity.findViewById(selectedId)
                            radioButton.text.toString()
                        }
                    }

                    ttsCtrl = TTSController(this@MainActivity, userTtsEngine)

                    debugLog("TTS Engine selected = $userTtsEngine")
                })
        engine_selector.removeAllViews()
        installedEngines.forEach {
            val radioButton = RadioButton(this)
            radioButton.text = it.name
            engine_selector.addView(radioButton)
        }

        // Speak buttons.
        key_q.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_q)) } )
        key_w.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_w)) } )
        key_e.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_e)) } )
        key_r.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_r)) } )
        key_t.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_t)) } )

        key_a.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_a)) } )
        key_s.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_s)) } )
        key_d.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_d)) } )
        key_f.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_f)) } )
        key_g.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_g)) } )
        key_h.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_h)) } )
        key_j.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_j)) } )
        key_k.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_k)) } )
        key_l.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_l)) } )

        key_z.setOnClickListener( { ttsCtrl?.speak(getString(R.string.key_z)) } )

        key_1.setOnClickListener( { soundPool.play(soundWan, 1.0f, 1.0f, 0, 0, 1.0f) } )
        key_2.setOnClickListener( { soundPool.play(soundKuun, 1.0f, 1.0f, 0, 0, 1.0f) } )
    }

    private fun speakInputText() {
        val text = input_text.text.toString()

        debugLog("speakInputText() : text = $text")

        ttsCtrl?.speak(text)
    }

    override fun onPause() {
        debugLog("onPause()")
        super.onPause()

        ttsCtrl?.release()
        ttsCtrl = null

        soundPool.release()
    }

    private val REQ_CODE_OVERLAY_PERMISSION: Int = 1000

    private fun isSystemAlertWindowPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    @SuppressLint("InlinedApi")
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



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String
}
