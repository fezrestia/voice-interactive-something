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
    }

    override fun onCreate() {
        super.onCreate()

        // SharedPreferences.
        gsp = getSharedPreferences(packageName, Context.MODE_PRIVATE)
    }

}
