package io.snabble.sdk.widgets.snabble.devsettings.repositories.di

import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepository
import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepositoryImpl
import io.snabble.sdk.widgets.snabble.devsettings.viewmodel.DevViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val devSettingsModule = module {
    viewModelOf(::DevViewModel)
    factoryOf(::DevSettingsRepositoryImpl) bind DevSettingsRepository::class
}
