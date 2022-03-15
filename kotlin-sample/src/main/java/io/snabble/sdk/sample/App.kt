package io.snabble.sdk.sample

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}