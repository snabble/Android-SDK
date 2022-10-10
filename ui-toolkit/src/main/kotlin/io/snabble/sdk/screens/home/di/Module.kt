package io.snabble.sdk.screens.home.di

import io.snabble.sdk.screens.home.domain.GetHomeConfigUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val homeModule = module {
    factoryOf(::GetHomeConfigUseCase)
}
