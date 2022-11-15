package io.snabble.sdk.widgets.snabble.wlan.di

import io.snabble.sdk.widgets.snabble.wlan.usecases.ConnectToWlanUseCase
import io.snabble.sdk.widgets.snabble.wlan.usecases.ConnectToWlanUseCaseImpl
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCaseImpl
import io.snabble.sdk.widgets.snabble.wlan.usecases.IsStoreWifiAvailable
import io.snabble.sdk.widgets.snabble.wlan.usecases.IsStoreWifiAvailableImpl
import io.snabble.sdk.widgets.snabble.wlan.usecases.wlanmanager.WlanManager
import io.snabble.sdk.widgets.snabble.wlan.usecases.wlanmanager.WlanManagerLegacyImpl
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WlanViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val wlanModule = module {
    viewModelOf(::WlanViewModel)
    factoryOf(::HasWlanConnectionUseCaseImpl) bind HasWlanConnectionUseCase::class
    factoryOf(::IsStoreWifiAvailableImpl) bind IsStoreWifiAvailable::class
    factoryOf(::ConnectToWlanUseCaseImpl) bind ConnectToWlanUseCase::class
    factoryOf(::WlanManagerLegacyImpl) bind WlanManager::class
}
