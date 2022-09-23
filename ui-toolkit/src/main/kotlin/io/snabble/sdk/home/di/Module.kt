package io.snabble.sdk.home.di

import io.snabble.sdk.home.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module {
    module {
        viewModelOf(::HomeViewModel)
    }
}
