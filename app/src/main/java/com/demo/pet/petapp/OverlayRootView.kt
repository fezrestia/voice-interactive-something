package com.demo.pet.petapp

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.overlay_root_view.view.*

class OverlayRootView : RelativeLayout {

    private val winMng: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val winParams: WindowManager.LayoutParams

    companion object {
        private var isOverlayActive = false

        public fun isActive(): Boolean {
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
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

    }

    fun addToOverlayWindow() {
        isOverlayActive = true
        winMng.addView(this, winParams)
    }

    fun removeFromOverlayWindow() {
        winMng.removeView(this)
        isOverlayActive = false
    }



    private class Pet(val targetView: ImageView) {
        val standDrawable = R.drawable.dog_stand_4
        val sitDrawable = R.drawable.dog_sit

        var currentDrawable = sitDrawable

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
//            debugLog("renderer.run() : E")

            if ((count / 30) % 2 == 0L) {
//                debugLog("pet.sit()")
                pet.sit()
            } else {
//                debugLog("pet.stand()")
                pet.stand()
            }





            ++count

            if (isActive) {
                handler.postDelayed(this, INTERVAL)
            }
//            debugLog("renderer.run() : X")
        }

    }

}
