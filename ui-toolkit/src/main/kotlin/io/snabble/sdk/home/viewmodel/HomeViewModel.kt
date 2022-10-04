package io.snabble.sdk.home.viewmodel

import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.ui.DynamicViewModel
import io.snabble.sdk.usecases.GetHomeConfigUseCase
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
