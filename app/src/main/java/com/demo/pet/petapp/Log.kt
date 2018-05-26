package com.demo.pet.petapp

object Log {
    const val IS_DEBUG = false
}

// Package level function.

fun debugLog(msg: String) {
    android.util.Log.e("TraceLog", msg)
}
