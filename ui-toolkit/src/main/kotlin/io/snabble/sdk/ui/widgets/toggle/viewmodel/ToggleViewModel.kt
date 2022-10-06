package io.snabble.sdk.ui.widgets.toggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.ui.widgets.toggle.repository.ToggleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ToggleViewModel(private val prefKey: String) : ViewModel() {

    private val _toggleState = MutableStateFlow(false)
    val toggleState: StateFlow<Boolean> = _toggleState.asStateFlow()

    private val repo: ToggleRepository by KoinProvider.getKoin().inject()

    init {
        viewModelScope.launch {
            repo.getToggleState(prefKey).collect(_toggleState::emit)
        }
    }

    fun setToggleState(id: String, isChecked: Boolean) {
        viewModelScope.launch {
            repo.saveToggleState(id, isChecked)
        }
    }
}

internal class ToggleViewModelFactory(private val prefKey: String) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ToggleViewModel(prefKey) as T
}
