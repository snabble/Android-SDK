package io.snabble.sdk.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.domain.Root
import io.snabble.sdk.usecases.GetHomeConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel internal constructor(
    private val getHomeConfig: GetHomeConfigUseCase,
) : ViewModel() {

    init {
        fetchHomeConfig()
    }

    private val _homeState: MutableStateFlow<UiState> = MutableStateFlow(Loading)
    val homeState: StateFlow<UiState> = _homeState

    private fun fetchHomeConfig() {
        viewModelScope.launch {
            val root = getHomeConfig()
            _homeState.value = Finished(root)
        }
    }
}

sealed class UiState
object Loading : UiState()
data class Finished(val root: Root) : UiState()
data class Error(val e: Exception) : UiState()
