package io.snabble.sdk.screens.home.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.screens.home.usecases.GetHomeConfigUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class DynamicHomeViewModel : DynamicViewModel() {

    private val getHomeConfig: GetHomeConfigUseCase by KoinProvider.inject()

    init {
        fetchHomeConfig()
    }

    private fun fetchHomeConfig() {
        viewModelScope.launch {
            val config = getHomeConfig()
            setConfig(config)
        }
    }
}
