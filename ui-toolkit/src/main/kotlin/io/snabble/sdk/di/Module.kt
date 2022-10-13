package io.snabble.sdk.di

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.preference.PreferenceManager
import io.snabble.sdk.Snabble
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    factory { Snabble }

    factory {
        androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    } bind WifiManager::class

    factory {
        androidContext().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    } bind ConnectivityManager::class

    factory { PreferenceManager.getDefaultSharedPreferences(androidContext()) } bind SharedPreferences::class
}
