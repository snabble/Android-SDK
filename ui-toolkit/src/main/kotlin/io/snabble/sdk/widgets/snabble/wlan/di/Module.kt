package io.snabble.sdk.widgets.snabble.wlan.di

import io.snabble.sdk.widgets.snabble.wlan.domain.GetAvailableWifiUseCase
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WifiViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val wifiModule = module {
    viewModelOf(::WifiViewModel)
    factoryOf(::GetAvailableWifiUseCase)
}
