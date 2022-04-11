package io.snabble.sdk.customization

import android.app.Application
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.google.android.material.color.DynamicColors

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Material You support
        DynamicColors.applyToActivitiesIfAvailable(this)

        val request = FontRequest(
            "com.example.fontprovider.authority",
            "com.example.fontprovider",
            "aclonica",
            R.array.com_google_android_gms_fonts_certs
        )

        val callback = FontsContractCompat.FontRequestCallback()
        FontsContractCompat.requestFont(this, request, callback, Handler(Looper.getMainLooper()))
    }
}