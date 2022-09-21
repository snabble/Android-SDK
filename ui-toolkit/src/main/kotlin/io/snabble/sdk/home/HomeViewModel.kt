package io.snabble.sdk.home

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.config.ConfigFileProviderImpl
import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.Root
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HomeViewModel : ViewModel() {

    companion object {
        val instance = HomeViewModel()
    }

    val checkInState: MutableState<Boolean>
        get() {
            var state = mutableStateOf(Snabble.currentCheckedInShop.value != null)
            Snabble.currentCheckedInShop.observeForever {
                state = mutableStateOf(it != null)
            }
            return state
        }

    var widgetEvent = MutableLiveData<String>()

    fun onClick(string: String) {
        widgetEvent.postValue(string)
    }

    private val state: MutableStateFlow<UiState> = MutableStateFlow(Loading)
    val homeState: StateFlow<UiState> = state

    fun fetchHomeConfig(context: Context){
        val repo = ConfigRepository(
            ConfigFileProviderImpl(context.resources.assets),
            Json
        )
        viewModelScope.launch {
            delay(3500)
            val rootDto = repo.getConfig<RootDto>("homeConfig.json")
            val root = ConfigMapperImpl(context).mapTo(rootDto)
            state.value = Finished(root)
        }
    }
}

sealed class UiState
object Loading : UiState()
data class Finished(val root: Root) : UiState()
data class Error(val e: Exception) : UiState()
