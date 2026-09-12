package com.autoreplytools.core.logging

import android.util.Log

class AppLogger(private val tag: String = "AutoReplyTools") {
    fun info(message: String) = Log.i(tag, message)

    fun warn(message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}