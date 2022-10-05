package io.snabble.sdk.ui.widgets.toggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.ui.widgets.toggle.repository.ToggleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ToggleViewModel : ViewModel() {

    private val _toggleState = MutableStateFlow(false)
    val toggleState: StateFlow<Boolean> = _toggleState.asStateFlow()

    private val repo: ToggleRepository by KoinProvider.getKoin().inject()

    fun setToggleState(id: String, isChecked: Boolean) {
        viewModelScope.launch {
            repo.saveToggleState(id, isChecked)
        }
    }

    fun setPrefKey(key: String) {
        viewModelScope.launch {
            repo.getToggleState(key).collect {
                _toggleState.emit(it)
            }
        }
    }
}
