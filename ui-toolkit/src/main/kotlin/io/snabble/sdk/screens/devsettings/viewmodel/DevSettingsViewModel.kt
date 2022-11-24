package io.snabble.sdk.screens.devsettings.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.screens.devsettings.usecases.GetDevSettingsConfigUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class DevSettingsViewModel : DynamicViewModel() {

    private val getDevSettingsConfig: GetDevSettingsConfigUseCase by KoinProvider.inject()

    init {
        fetchDevSettingsConfig()
    }

    private fun fetchDevSettingsConfig() {
        viewModelScope.launch {
            val config = getDevSettingsConfig()
            setConfig(config)
        }
    }
}
