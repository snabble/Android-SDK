package io.snabble.sdk.ui.widgets.stores.di

import io.snabble.sdk.ui.widgets.stores.viewmodel.StoresViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val storesModule = module {
    viewModelOf(::StoresViewModel)
}
