package com.presenceprotocol.app

import android.app.Application
import android.content.Context

class PresenceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
