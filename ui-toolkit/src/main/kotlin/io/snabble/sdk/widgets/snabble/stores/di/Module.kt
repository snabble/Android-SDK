package io.snabble.sdk.widgets.snabble.stores.di

import io.snabble.sdk.widgets.snabble.stores.viewmodel.StoresViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val storesModule = module {
    viewModelOf(::StoresViewModel)
}
