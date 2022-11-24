package io.snabble.sdk.widgets.snabble.devsettings.repositories.di

import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepository
import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepositoryImpl
import io.snabble.sdk.widgets.snabble.devsettings.usecase.HasEnableDevSettingsUseCase
import io.snabble.sdk.widgets.snabble.devsettings.usecase.HasEnableDevSettingsUseCaseImpl
import io.snabble.sdk.widgets.snabble.devsettings.viewmodel.DevViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val devSettingsModule = module {
    viewModelOf(::DevViewModel)

    factoryOf(::HasEnableDevSettingsUseCaseImpl) bind HasEnableDevSettingsUseCase::class

    factory<DevSettingsRepository> {
        DevSettingsRepositoryImpl(
            sharedPreferences = get(),
            androidContext().getString(R.string.dev_settings_password)
        )
    }
}
