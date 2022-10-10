package io.snabble.sdk.widgets.snabble.wifi.di

import io.snabble.sdk.widgets.snabble.wifi.domain.GetAvailableWifiUseCase
import io.snabble.sdk.widgets.snabble.wifi.viewmodel.WifiViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val wifiModule = module {
    viewModelOf(::WifiViewModel)
    factoryOf(::GetAvailableWifiUseCase)
}
