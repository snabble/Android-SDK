package io.snabble.sdk.sample

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // To enable Material You support
        // DynamicColors.applyToActivitiesIfAvailable(this)
    }
}