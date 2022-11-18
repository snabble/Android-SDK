package io.snabble.sdk.wlanmanager.di

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import io.snabble.sdk.wlanmanager.WlanManager
import io.snabble.sdk.wlanmanager.WlanManagerImpl
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.KEY_SUGGESTIONS
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.PREFS_WLAN
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.HasScanFinished
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.ScanIsFinishedImpl
import io.snabble.sdk.wlanmanager.usecase.connect.ConnectToWlanUseCase
import io.snabble.sdk.wlanmanager.usecase.connect.ConnectToWlanUseCaseApi29
import io.snabble.sdk.wlanmanager.usecase.connect.ConnectToWlanUseCaseApi30
import io.snabble.sdk.wlanmanager.usecase.connect.ConnectToWlanUseCaseLegacy
import io.snabble.sdk.wlanmanager.usecase.scan.ScanForNetworkUseCase
import io.snabble.sdk.wlanmanager.usecase.scan.ScanForNetworkUseCaseApi28
import io.snabble.sdk.wlanmanager.usecase.scan.ScanForNetworkUseCaseApi29
import io.snabble.sdk.wlanmanager.usecase.scan.ScanForNetworkUseCaseLegacy
import io.snabble.sdk.wlanmanager.usecase.availability.IsWlanSsidAvailableUseCase
import io.snabble.sdk.wlanmanager.usecase.availability.IsWlanSsidAvailableUseCaseApi28
import io.snabble.sdk.wlanmanager.usecase.availability.IsWlanSsidAvailableUseCaseApi29
import io.snabble.sdk.wlanmanager.usecase.availability.IsWlanSsidAvailableUseCaseLegacy
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

    factoryOf(::ScanIsFinishedImpl) bind HasScanFinished::class

    factory<ScanForNetworkUseCase> {
        when (Build.VERSION.SDK_INT) {
            in 1..27 -> ScanForNetworkUseCaseLegacy(wifiManager = get())
            28 -> ScanForNetworkUseCaseApi28(context = get(), wifiManager = get())
            else -> ScanForNetworkUseCaseApi29(context = get(), wifiManager = get())
        }
    }

    factory<IsWlanSsidAvailableUseCase> {
        when (Build.VERSION.SDK_INT) {
            in 1..27 -> IsWlanSsidAvailableUseCaseLegacy(context = get(), wifiManager = get())
            28 -> IsWlanSsidAvailableUseCaseApi28(context = get(), wifiManager = get())
            else -> IsWlanSsidAvailableUseCaseApi29(context = get(), wifiManager = get())
        }
    }

    factory<ConnectToWlanUseCase> {
        when (Build.VERSION.SDK_INT) {
            in 1..28 -> ConnectToWlanUseCaseLegacy(wifiManager = get())
            29 -> ConnectToWlanUseCaseApi29(sharedPrefs = get(named(PREFS_WLAN)), wifiManager = get())
            else -> ConnectToWlanUseCaseApi30(sharedPrefs = get(named(PREFS_WLAN)), wifiManager = get())
        }
    }
}
