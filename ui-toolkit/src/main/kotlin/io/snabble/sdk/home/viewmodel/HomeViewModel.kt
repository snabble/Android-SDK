package io.snabble.sdk.home.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.domain.Root
import io.snabble.sdk.ui.DynamicViewModel
import io.snabble.sdk.usecases.GetCustomerCardInfo
import io.snabble.sdk.usecases.GetHomeConfigUseCase
import io.snabble.sdk.usecases.GetPermissionStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel internal constructor(
    getPermissionState: GetPermissionStateUseCase,
    private val getHomeConfig: GetHomeConfigUseCase,
    getCustomerCardInfo: GetCustomerCardInfo,
    // dynamicViewModel: DynamicViewModel,
) : ViewModel() {

    init {
        fetchHomeConfig()
    }

    var permissionState = getPermissionState()

    val checkInState: MutableState<Boolean>
        get() {
            val state = mutableStateOf(Snabble.currentCheckedInShop.value != null)
            Snabble.currentCheckedInShop.observeForever { shop ->
                state.value = shop != null
            }
            return state
        }

    val customerCardVisibilityState =
        getCustomerCardInfo() // FIXME: MutableState visible from outside

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
