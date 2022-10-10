package io.snabble.sdk.widgets.snabble.customercard.di


import io.snabble.sdk.widgets.snabble.customercard.domain.HasCustomerCardUseCase
import io.snabble.sdk.widgets.snabble.customercard.viewmodel.CustomerCardViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val customerCardModule = module {
    viewModelOf(::CustomerCardViewModel)
    factoryOf(::HasCustomerCardUseCase)
}
