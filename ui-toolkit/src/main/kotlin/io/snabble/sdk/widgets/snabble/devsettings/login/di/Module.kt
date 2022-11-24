package io.snabble.sdk.widgets.snabble.devsettings.login.di

import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.devsettings.login.repositories.DevSettingsLoginRepository
import io.snabble.sdk.widgets.snabble.devsettings.login.repositories.DevSettingsLoginRepositoryImpl
import io.snabble.sdk.widgets.snabble.devsettings.login.usecase.HasEnabledDevSettingsUseCase
import io.snabble.sdk.widgets.snabble.devsettings.login.usecase.HasEnabledDevSettingsUseCaseImpl
import io.snabble.sdk.widgets.snabble.devsettings.login.viewmodel.DevSettingsLoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val devSettingsModule = module {
    viewModelOf(::DevSettingsLoginViewModel)

    factoryOf(::HasEnabledDevSettingsUseCaseImpl) bind HasEnabledDevSettingsUseCase::class

    factory<DevSettingsLoginRepository> {
        DevSettingsLoginRepositoryImpl(
            sharedPreferences = get(),
            devSettingPassword = androidContext().getString(R.string.dev_settings_password),
        )
    }
}
