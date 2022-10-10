package io.snabble.sdk.screens.profile.di

import io.snabble.sdk.screens.profile.domain.GetProfileConfigUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val profileModule = module {
    factoryOf(::GetProfileConfigUseCase)
}
