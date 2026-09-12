package com.autoreplytools

import android.app.Application
import com.autoreplytools.core.RuntimeContainer

class AutoReplyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeContainer.initialize(this)
    }
}