package io.snabble.sdk.screens.profile.di

import io.snabble.sdk.screens.profile.usecases.GetProfileConfigUseCase
import io.snabble.sdk.screens.profile.usecases.GetProfileConfigUseCaseImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val profileModule = module {
    factoryOf(::GetProfileConfigUseCaseImpl) bind GetProfileConfigUseCase::class
}
