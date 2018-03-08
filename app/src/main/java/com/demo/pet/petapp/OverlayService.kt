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

const val REQUEST_START_OVERLAY = "com.demo.pet.petapp.action.REQUEST_START_OVERLAY"
const val REQUEST_STOP_OVERLAY = "com.demo.pet.petapp.action.REQUEST_STOP_OVERLAY"

class OverlayService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val NOTIFICATION_CHANNEL_ONGOING = "ongoing"
    private val NOTIFICATION_ID = 1000

    private var rootView: OverlayRootView? = null

    @SuppressLint("NewApi")
    override fun onCreate() {
        debugLog("OverlayService.onCreate()")
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

        // Foreground service.
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        debugLog("OverlayService.onStartCommand()")

        val action = intent.action

        if (action == null) {
            debugLog("Unexpected Intent action = null")
        } else when (action) {
            REQUEST_START_OVERLAY -> {
                rootView = View.inflate(this, R.layout.overlay_root_view, null) as OverlayRootView
                rootView?.initialize()
                rootView?.addToOverlayWindow()
            }

            REQUEST_STOP_OVERLAY -> {
                rootView?.release()
                rootView?.removeFromOverlayWindow()
                rootView = null

                stopSelf()
            }

            else -> {
                throw RuntimeException("Unexpected action = $action")
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        debugLog("OverlayService.onDestroy()")
        super.onDestroy()

        stopForeground(true)

    }
}
