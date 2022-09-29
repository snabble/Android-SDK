package io.snabble.sdk.ui.widgets.customercard.di

import io.snabble.sdk.ui.widgets.customercard.viewmodel.CustomerCardViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val customerCardModule = module {
    viewModelOf(::CustomerCardViewModel)
}
