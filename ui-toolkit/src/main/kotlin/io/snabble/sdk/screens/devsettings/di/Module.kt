package io.snabble.sdk.screens.devsettings.di

import io.snabble.sdk.screens.devsettings.usecases.GetDevSettingsConfigUseCase
import io.snabble.sdk.screens.devsettings.usecases.GetDevSettingsConfigUseCaseImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val devSettingsModule = module {
    factoryOf(::GetDevSettingsConfigUseCaseImpl) bind GetDevSettingsConfigUseCase::class
}
