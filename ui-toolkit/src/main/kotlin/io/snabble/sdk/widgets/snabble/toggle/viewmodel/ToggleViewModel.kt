package io.snabble.sdk.widgets.snabble.toggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.toggle.usecases.GetToggleStateUseCase
import io.snabble.sdk.widgets.snabble.toggle.usecases.SaveToggleStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class ToggleViewModel(
    getToggleState: GetToggleStateUseCase,
    private val saveToggle: SaveToggleStateUseCase,
) : ViewModel() {

    private val _toggleState = MutableStateFlow(false)
    val toggleState: StateFlow<Boolean> = _toggleState.asStateFlow()

    init {
        getToggleState()
            .stateIn(
                scope = viewModelScope,
                initialValue = false,
                started = SharingStarted.WhileSubscribed(5000),
            )
            .onEach(_toggleState::emit)
            .launchIn(viewModelScope)
    }

    fun saveToggleState(isChecked: Boolean) {
        saveToggle(isChecked)
    }
}
