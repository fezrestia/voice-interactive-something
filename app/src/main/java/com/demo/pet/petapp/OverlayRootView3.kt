@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import kotlinx.android.synthetic.main.overlay_root_view.view.debug_msg
import kotlinx.android.synthetic.main.overlay_root_view.view.pet_icon
import kotlinx.android.synthetic.main.overlay_root_view.view.voice_level

class OverlayRootView3 : RelativeLayout {
    private val IS_DEBUG = Log.IS_DEBUG || false

    private val winMng: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val winParams: WindowManager.LayoutParams

    lateinit var pet: Pet
    private lateinit var renderer: RenderingTask

    private val uiHandler: Handler

    var isActive: Boolean = false

    init {
        var overlayType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        // Immersive mode.
        systemUiVisibility = (SYSTEM_UI_FLAG_FULLSCREEN
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_IMMERSIVE)

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

    }

    constructor(context: Context) : super(context) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_1")
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_2")
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_3")
    }

    fun initialize() {
        // Cache instances.
        pet = Pet(pet_icon)

        // Renderer.
        renderer = RenderingTask(pet, uiHandler)
        renderer.start()

    }

    fun release() {
        renderer.stop()

    }

    fun getDebugMsg(): TextView {
        return debug_msg
    }

    fun getVoiceLevel(): View {
        return voice_level
    }

    fun addToOverlayWindow() {
        isActive = true
        winMng.addView(this, winParams)
    }

    fun removeFromOverlayWindow() {
        winMng.removeView(this)
        isActive = false
    }

    inner class Pet(private val targetView: ImageView) {
        private val standDrawable = R.drawable.dog_stand_4
        private val sitDrawable = R.drawable.dog_sit
        private val sitSpeakDrawable = R.drawable.dog_sit_speaking

        private var currentDrawable = sitDrawable

        private var isSpeaking = false

        init {
            targetView.setImageResource(currentDrawable)
        }

        private fun draw() {
            targetView.setImageResource(currentDrawable)
        }

        private fun sit() {
            if (currentDrawable != sitDrawable) {
                currentDrawable = sitDrawable
                draw()
            }
        }

        private fun sitSpeak() {
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
        }

        fun startSpeak() {
            isSpeaking = true
        }

        fun stopSpeak() {
            isSpeaking = false
        }
    }

    private inner class RenderingTask(val pet: Pet, val handler: Handler) : Runnable {
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
            if (IS_DEBUG) debugLog("renderer.run() : E")

            pet.render(count)

            ++count

            if (isActive) {
                handler.postDelayed(this, INTERVAL)
            }
            if (IS_DEBUG) debugLog("renderer.run() : X")
        }

    }

}
