package io.snabble.sdk.ui.widgets.stores.di

import io.snabble.sdk.ui.widgets.stores.StoresViewModel
import io.snabble.sdk.ui.widgets.stores.WifiViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val storesModule = module {
    viewModelOf(::StoresViewModel)
}
