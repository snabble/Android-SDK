package io.snabble.sdk.screens.profile.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.screens.profile.usecases.GetProfileConfigUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class DynamicProfileViewModel : DynamicViewModel() {

    private val getProfileConfig: GetProfileConfigUseCase by KoinProvider.inject()

    init {
        fetchHomeConfig()
    }

    private fun fetchHomeConfig() {
        viewModelScope.launch {
            val config = getProfileConfig()
            setConfig(config)
        }
    }
}
