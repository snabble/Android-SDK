@file:Suppress("RemoveExplicitTypeArguments")

package io.snabble.sdk.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import io.snabble.sdk.Snabble
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    factory { Snabble }
    factory<WifiManager> { androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    factory<ConnectivityManager> { androidContext().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
}
