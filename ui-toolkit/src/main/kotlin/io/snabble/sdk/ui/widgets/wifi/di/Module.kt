package io.snabble.sdk.ui.widgets.wifi.di

import io.snabble.sdk.ui.widgets.stores.WifiViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val wifiModule = module {
    viewModelOf(::WifiViewModel)
}
