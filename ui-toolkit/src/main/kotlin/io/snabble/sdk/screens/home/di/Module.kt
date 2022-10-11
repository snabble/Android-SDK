package io.snabble.sdk.screens.home.di

import io.snabble.sdk.screens.home.domain.GetHomeConfigUseCase
import io.snabble.sdk.screens.home.domain.GetHomeConfigUseCaseImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val homeModule = module {
    factoryOf(::GetHomeConfigUseCaseImpl) bind GetHomeConfigUseCase::class
}
