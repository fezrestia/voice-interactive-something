package com.demo.pet.petapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class PetApplication : Application() {

    companion object {
        private lateinit var gsp: SharedPreferences

        fun getSP(): SharedPreferences {
            return gsp
        }

        var isKatchy3Active = false

    }

    override fun onCreate() {
        super.onCreate()

        // SharedPreferences.
        gsp = getSharedPreferences(packageName, Context.MODE_PRIVATE)

        // Check shared preferences version.
        val storedVer = gsp.getInt(Constants.KEY_VERSION, Constants.VAL_INVALID_VERSION)
        if (storedVer != Constants.VAL_VERSION) {
            // Version is updated. Backward compatibility is broken. Clean ALL.
            gsp.edit().clear().apply()
            // Store version.
            gsp.edit().putInt(Constants.KEY_VERSION, Constants.VAL_VERSION).apply()
        }

    }

}
