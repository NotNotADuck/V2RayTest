package com.example.v2raytest

import android.app.Application
import dev.dev7.lib.v2ray.V2rayController

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // It could be in the MainActivity but I like it to stay here.
        V2rayController.init(this, R.drawable.ic_launcher, "V2ray Example")
    }
}