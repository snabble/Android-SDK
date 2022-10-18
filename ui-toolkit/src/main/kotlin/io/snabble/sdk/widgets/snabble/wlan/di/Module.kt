package io.snabble.sdk.widgets.snabble.wlan.di

import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCaseImpl
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WlanViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val wlanModule = module {
    viewModelOf(::WlanViewModel)
    factoryOf(::HasWlanConnectionUseCaseImpl) bind HasWlanConnectionUseCase::class
}
