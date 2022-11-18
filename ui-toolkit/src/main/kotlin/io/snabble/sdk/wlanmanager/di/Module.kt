package io.snabble.sdk.wlanmanager.di

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import io.snabble.sdk.wlanmanager.WlanManager
import io.snabble.sdk.wlanmanager.WlanManagerImpl
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.KEY_SUGGESTIONS
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.PREFS_WLAN
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.ScanIsFinished
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.ScanIsFinishedImpl
import io.snabble.sdk.wlanmanager.usecase.connectNetwork.ConnectToWifi
import io.snabble.sdk.wlanmanager.usecase.connectNetwork.ConnectToWifiApi29
import io.snabble.sdk.wlanmanager.usecase.connectNetwork.ConnectToWifiApi30
import io.snabble.sdk.wlanmanager.usecase.connectNetwork.ConnectToWifiLegacy
import io.snabble.sdk.wlanmanager.usecase.networkscan.ScanForNetwork
import io.snabble.sdk.wlanmanager.usecase.networkscan.ScanForNetworkApi28
import io.snabble.sdk.wlanmanager.usecase.networkscan.ScanForNetworkApi29
import io.snabble.sdk.wlanmanager.usecase.networkscan.ScanForNetworkLegacy
import io.snabble.sdk.wlanmanager.usecase.ssidcheck.CheckSsid
import io.snabble.sdk.wlanmanager.usecase.ssidcheck.CheckSsidApi28
import io.snabble.sdk.wlanmanager.usecase.ssidcheck.CheckSsidApi29
import io.snabble.sdk.wlanmanager.usecase.ssidcheck.CheckSsidLegacy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val wlanManagerModule = module {
    factory<SharedPreferences>(named(PREFS_WLAN)) {
        androidContext().getSharedPreferences(KEY_SUGGESTIONS, MODE_PRIVATE)
    }

    factoryOf(::WlanManagerImpl) bind WlanManager::class

    factoryOf(::ScanIsFinishedImpl) bind ScanIsFinished::class

    factory<ScanForNetwork> {
        when (Build.VERSION.SDK_INT) {
            in 1..27 -> ScanForNetworkLegacy(wifiManager = get())
            28 -> ScanForNetworkApi28(context = get(), wifiManager = get())
            else -> ScanForNetworkApi29(context = get(), wifiManager = get())
        }
    }

    factory<CheckSsid> {
        when (Build.VERSION.SDK_INT) {
            in 1..27 -> CheckSsidLegacy(context = get(), wifiManager = get())
            28 -> CheckSsidApi28(context = get(), wifiManager = get())
            else -> CheckSsidApi29(context = get(), wifiManager = get())
        }
    }

    factory<ConnectToWifi> {
        when (Build.VERSION.SDK_INT) {
            in 1..28 -> ConnectToWifiLegacy(wifiManager = get())
            29 -> ConnectToWifiApi29(sharedPrefs = get(named(PREFS_WLAN)), wifiManager = get())
            else -> ConnectToWifiApi30(sharedPrefs = get(named(PREFS_WLAN)), wifiManager = get())
        }
    }
}
