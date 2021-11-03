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

        // if you are using a light mode theme, disable night mode resources
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        instance = this
    }
}