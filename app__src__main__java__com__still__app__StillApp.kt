package com.still.app

import android.app.Application

class StillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        // Start loading apps and icons the moment the process starts,
        // so the lists are usually ready before any tab opens.
        Apps.preload(this)
    }
}
