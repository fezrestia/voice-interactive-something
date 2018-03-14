package com.demo.pet.petapp

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.overlay_root_view.view.*

class OverlayRootView : RelativeLayout {

    private val winMng: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val winParams: WindowManager.LayoutParams

    private val ttsCtrl: TTSController

    private val soundPool: SoundPool
    private val soundWan: Int
    private val soundKuun: Int

    companion object {
        private var isOverlayActive = false

        fun isActive(): Boolean {
            return isOverlayActive
        }
    }

    private lateinit var pet: Pet
    private lateinit var renderer: RenderingTask

    private val uiHandler: Handler

    init {
        var overlayType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        winParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT)
        winParams.gravity = Gravity.BOTTOM
        winParams.y = 0

        // UI thread.
        uiHandler = Handler()

        // TTS.
        ttsCtrl = TTSController(context, MainActivity.userTtsEngine, SpeakStateCallbackImpl())

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

        soundWan = soundPool.load(context, R.raw.wan_wan, 1)
        soundKuun = soundPool.load(context, R.raw.wan_kuun, 1)
    }

    constructor(context: Context) : super(context) {
        debugLog("OverlayRootView.CONSTRUCTOR_1")
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        debugLog("OverlayRootView.CONSTRUCTOR_2")
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        debugLog("OverlayRootView.CONSTRUCTOR_3")
    }

    fun initialize() {
        // Cache instances.
        pet = Pet(pet_icon)
        pet.sit()

        // Renderer.
        renderer = RenderingTask(pet, uiHandler)
        renderer.start()

    }

    fun release() {
        renderer.stop()
        ttsCtrl.release()
        soundPool.release()
    }

    fun addToOverlayWindow() {
        isOverlayActive = true
        winMng.addView(this, winParams)
    }

    fun removeFromOverlayWindow() {
        winMng.removeView(this)
        isOverlayActive = false
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN) {
            debugLog("onKeyDown() : KEYCODE = ${event.displayLabel}")

            when (event.keyCode) {
                KeyEvent.KEYCODE_Q -> { ttsCtrl.speak(context.getString(R.string.key_q)) }
                KeyEvent.KEYCODE_W -> { ttsCtrl.speak(context.getString(R.string.key_w)) }
                KeyEvent.KEYCODE_E -> { ttsCtrl.speak(context.getString(R.string.key_e)) }
                KeyEvent.KEYCODE_R -> { ttsCtrl.speak(context.getString(R.string.key_r)) }
                KeyEvent.KEYCODE_T -> { ttsCtrl.speak(context.getString(R.string.key_t)) }

                KeyEvent.KEYCODE_A -> { ttsCtrl.speak(context.getString(R.string.key_a)) }
                KeyEvent.KEYCODE_S -> { ttsCtrl.speak(context.getString(R.string.key_s)) }
                KeyEvent.KEYCODE_D -> { ttsCtrl.speak(context.getString(R.string.key_d)) }
                KeyEvent.KEYCODE_F -> { ttsCtrl.speak(context.getString(R.string.key_f)) }
                KeyEvent.KEYCODE_G -> { ttsCtrl.speak(context.getString(R.string.key_g)) }
                KeyEvent.KEYCODE_H -> { ttsCtrl.speak(context.getString(R.string.key_h)) }
                KeyEvent.KEYCODE_J -> { ttsCtrl.speak(context.getString(R.string.key_j)) }
                KeyEvent.KEYCODE_K -> { ttsCtrl.speak(context.getString(R.string.key_k)) }
                KeyEvent.KEYCODE_L -> { ttsCtrl.speak(context.getString(R.string.key_l)) }

                KeyEvent.KEYCODE_Z -> { ttsCtrl.speak(context.getString(R.string.key_z)) }

                KeyEvent.KEYCODE_1 -> { soundPool.play(soundWan, 1.0f, 1.0f, 0, 0, 1.0f) }
                KeyEvent.KEYCODE_2 -> { soundPool.play(soundKuun, 1.0f, 1.0f, 0, 0, 1.0f) }

                else -> {
                    debugLog("onKeyDown() : OTHER")
                    return false
                }
            }
        }

        return true
    }

    private inner class SpeakStateCallbackImpl : TTSController.SpeakStateCallback {
        override fun onStarted() {
            pet.startSpeak()
        }

        override fun onCompleted(isSucceeded: Boolean) {
            pet.stopSpeak()
        }
    }

    private class Pet(val targetView: ImageView) {
        val standDrawable = R.drawable.dog_stand_4
        val sitDrawable = R.drawable.dog_sit
        val sitSpeakDrawable = R.drawable.dog_sit_speaking

        var currentDrawable = sitDrawable

        var isSpeaking = false

        init {
            targetView.setImageResource(currentDrawable)
        }

        private fun draw() {
            targetView.setImageResource(currentDrawable)
        }

        fun stand() {
            if (currentDrawable != standDrawable) {
                currentDrawable = standDrawable
                draw()
            }
        }

        fun sit() {
            if (currentDrawable != sitDrawable) {
                currentDrawable = sitDrawable
                draw()
            }
        }

        fun sitSpeak() {
            if (currentDrawable != sitSpeakDrawable) {
                currentDrawable = sitSpeakDrawable
                draw()
            }
        }

        fun render(count: Long) {
            if (isSpeaking) {
                if ((count / 5L) % 2 == 0L) {
                    sit()
                } else {
                    sitSpeak()
                }
            } else {
                sit()
            }

/* DEMO
            if ((count / 30) % 2 == 0L) {
                debugLog("pet.sit()")
                pet.sit()
            } else {
                debugLog("pet.stand()")
                pet.stand()
            }
*/
        }

        fun startSpeak() {
            isSpeaking = true
        }

        fun stopSpeak() {
            isSpeaking = false
        }
    }

    private class RenderingTask(val pet: Pet, val handler: Handler) : Runnable {
        private var isActive = true
        private val INTERVAL = 1000L / 30
        private var count: Long = 0

        fun start() {
            handler.postDelayed(this, INTERVAL)
        }

        fun stop() {
            isActive = false
            handler.removeCallbacks(this)
        }

        override fun run() {
            debugLog("renderer.run() : E")

            pet.render(count)

            ++count

            if (isActive) {
                handler.postDelayed(this, INTERVAL)
            }
//            debugLog("renderer.run() : X")
        }

    }

}
