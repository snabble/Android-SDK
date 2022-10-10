package io.snabble.sdk.screens.home.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.screens.home.domain.GetHomeConfigUseCase
import kotlinx.coroutines.launch

class DynamicHomeViewModel : DynamicViewModel() {

    private val getHomeConfig: GetHomeConfigUseCase by lazy { KoinProvider.getKoin().get() }

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
