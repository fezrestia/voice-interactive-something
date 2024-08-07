@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp.character

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.R
import com.demo.pet.petapp.util.debugLog
import com.demo.pet.petapp.util.errorLog

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

        model = Model(rootView.findViewById(R.id.model))
        renderer = Renderer(model, Handler(Looper.getMainLooper()))

        // Window params for overlay
        val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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

    override fun startSpeak() {
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
        rootView.findViewById<TextView>(R.id.debug_msg).text = msg
    }

    override fun updateVoiceLevel(level: Int, min: Int, max: Int) {
        val rate = level.toFloat() / (max.toFloat() - min.toFloat())
        val voiceLevel = rootView.findViewById<View>(R.id.voice_level)
        voiceLevel.pivotX = 0.0f
        voiceLevel.scaleX = rate
    }

    override fun changeVoiceLevelToIdle() {
        rootView.findViewById<View>(R.id.voice_level).setBackgroundColor(Color.WHITE)
    }

    override fun changeVoiceLevelToRec() {
        rootView.findViewById<View>(R.id.voice_level).setBackgroundColor(Color.RED)
    }
}
