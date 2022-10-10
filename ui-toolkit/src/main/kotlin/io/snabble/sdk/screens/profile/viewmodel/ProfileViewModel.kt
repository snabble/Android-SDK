package io.snabble.sdk.screens.profile.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.screens.profile.domain.GetProfileConfigUseCase
import kotlinx.coroutines.launch

class DynamicProfileViewModel : DynamicViewModel() {

    private val getProfileConfig: GetProfileConfigUseCase by lazy { KoinProvider.getKoin().get() }

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
