@file:Suppress(
        "SimplifyBooleanWithConstants",
        "PrivatePropertyName",
        "ConstantConditionIf")

package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import com.demo.pet.petapp.activespeak.FaceTrigger

const val REQUEST_START_OVERLAY = "com.demo.pet.petapp.action.REQUEST_START_OVERLAY"
const val REQUEST_STOP_OVERLAY = "com.demo.pet.petapp.action.REQUEST_STOP_OVERLAY"

class OverlayService : Service() {

    private val IS_DEBUG = false || Log.IS_DEBUG

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val NOTIFICATION_CHANNEL_ONGOING = "ongoing"
    private val NOTIFICATION_ID = 1000

    private var rootView: OverlayRootView? = null

    private var faceTrigger: FaceTrigger? = null

    @SuppressLint("NewApi")
    override fun onCreate() {
        if (IS_DEBUG) debugLog("OverlayService.onCreate()")
        super.onCreate()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ONGOING,
                    "overlay on-going",
                    NotificationManager.IMPORTANCE_MIN)
            manager.createNotificationChannel(channel)

            Notification.Builder(this, NOTIFICATION_CHANNEL_ONGOING)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
                .setContentTitle(getText(R.string.app_name))
                .setSmallIcon(R.drawable.dog_sit)
                .build()

        // Foreground service.
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (IS_DEBUG) debugLog("OverlayService.onStartCommand()")

        val action = intent.action

        if (action == null) {
            if (IS_DEBUG) debugLog("Unexpected Intent action = null")
        } else when (action) {
            REQUEST_START_OVERLAY -> {
                val view = View.inflate(this, R.layout.overlay_root_view, null) as OverlayRootView
                view.initialize()
                view.addToOverlayWindow()

                val trigger = FaceTrigger(this)
                trigger.resume()

                view.setFaceTrigger(trigger)

                rootView = view
                faceTrigger = trigger
            }

            REQUEST_STOP_OVERLAY -> {
                rootView?.release()
                rootView?.removeFromOverlayWindow()
                rootView?.setFaceTrigger(null)
                rootView = null

                faceTrigger?.pause()
                faceTrigger?.release()
                faceTrigger = null

                stopSelf()
            }

            else -> {
                throw RuntimeException("Unexpected action = $action")
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("OverlayService.onDestroy()")
        super.onDestroy()

        stopForeground(true)

    }
}
