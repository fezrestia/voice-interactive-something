@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp.character

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import com.demo.pet.petapp.Log
import com.demo.pet.petapp.R
import com.demo.pet.petapp.debugLog
import com.demo.pet.petapp.errorLog
import kotlinx.android.synthetic.main.character_katchy_dog.view.*

class CharacterKatchyDog(val context: Context) : Character {
    private val IS_DEBUG = Log.IS_DEBUG || false

    private val winMng = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var winParams: WindowManager.LayoutParams

    private lateinit var rootView: RelativeLayout

    private lateinit var model: Model
    private lateinit var renderer: Renderer

    override var isOnOverlay = false

    override fun initialize() {
        rootView = View.inflate(
                context,
                R.layout.character_katchy_dog,
                null) as RelativeLayout

        model = Model(rootView.model)
        renderer = Renderer(model, Handler())

        // Window params for overlay
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        winParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT)
        winParams.gravity = Gravity.BOTTOM
        winParams.y = 0

        // Immersive mode.
        rootView.systemUiVisibility = (RelativeLayout.SYSTEM_UI_FLAG_FULLSCREEN
                or RelativeLayout.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or RelativeLayout.SYSTEM_UI_FLAG_IMMERSIVE)

    }

    override fun addToOverlayWindow() {
        if (isOnOverlay) {
            errorLog("addToOverlayWindow() : Error, already on overlay.")
            return
        }

        isOnOverlay = true
        winMng.addView(rootView, winParams)

        renderer.start()
    }

    override fun removeFromOverlayWindow() {
        if (!isOnOverlay) {
            errorLog("removeFromOverlayWindow() : Error, already NOT on overlay.")
            return
        }

        winMng.removeView(rootView)
        isOnOverlay = false

        renderer.stop()
    }

    override fun release() {
        renderer.stop()
    }

    override fun startSpeack() {
        model.startSpeak()
    }

    override fun stopSpeak() {
        model.stopSpeak()
    }

    inner class Model(private val targetView: ImageView) {
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

    private inner class Renderer(val model: Model, val handler: Handler) : Runnable {
        private val INTERVAL = 1000L / 30
        private var count: Long = 0
        private var isActive = true

        fun start() {
            handler.postDelayed(this, INTERVAL)
        }

        fun stop() {
            isActive = false
            handler.removeCallbacks(this)
        }

        override fun run() {
            if (IS_DEBUG) debugLog("renderer.run()")

            model.render(count)

            ++count

            if (isActive) {
                handler.postDelayed(this, INTERVAL)
            }
        }
    }

    //// For DEBUG

    override fun updateDebugMsg(msg: String) {
    }

    override fun updateVoiceLevel(level: Int, min: Int, max: Int) {
        val rate = level.toFloat() / (max.toFloat() - min.toFloat())
        rootView.voice_level.pivotX = 0.0f
        rootView.voice_level.scaleX = rate
    }

    override fun changeVoiceLevelToIdle() {
        rootView.voice_level.setBackgroundColor(Color.WHITE)
    }

    override fun changeVoiceLevelToRec() {
        rootView.voice_level.setBackgroundColor(Color.RED)
    }
}
