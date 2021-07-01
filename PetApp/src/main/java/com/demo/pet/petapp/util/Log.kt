package com.demo.pet.petapp.util

object Log {
    const val IS_DEBUG = false
}

// Package level function.

fun debugLog(msg: String) {
    android.util.Log.e("TraceLog", msg)
}

fun errorLog(msg: String) {
    android.util.Log.e("TraceLog", "ERROR: $msg")
}
