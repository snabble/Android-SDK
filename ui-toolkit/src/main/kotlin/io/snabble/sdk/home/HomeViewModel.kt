package io.snabble.sdk.home

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.domain.Root
import io.snabble.sdk.usecase.GetHomeConfigUseCase
import io.snabble.sdk.usecase.GetPermissionStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var permissionState = GetPermissionStateUseCase()()

    val checkInState: MutableState<Boolean>
        get() {
            val state = mutableStateOf(Snabble.currentCheckedInShop.value != null)
            Snabble.currentCheckedInShop.observeForever { shop ->
                state.value = shop != null
            }
            return state
        }

    var widgetEvent = MutableLiveData<String>()

    fun onClick(string: String) {
        widgetEvent.postValue(string)
    }

    private val _homeState: MutableStateFlow<UiState> = MutableStateFlow(Loading)
    val homeState: StateFlow<UiState> = _homeState

    fun fetchHomeConfig(context: Context) {
        viewModelScope.launch {
            val root = GetHomeConfigUseCase()(context)
            _homeState.value = Finished(root)
        }
    }
}

sealed class UiState
object Loading : UiState()
data class Finished(val root: Root) : UiState()
data class Error(val e: Exception) : UiState()
