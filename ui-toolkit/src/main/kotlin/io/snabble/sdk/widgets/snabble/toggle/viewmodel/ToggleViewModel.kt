package io.snabble.sdk.widgets.snabble.toggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.toggle.usecases.GetToggleStateUseCase
import io.snabble.sdk.widgets.snabble.toggle.usecases.SetToggleStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ToggleViewModel(
    private val getToggleState: GetToggleStateUseCase,
    private val setToggle: SetToggleStateUseCase,
) : ViewModel() {

    private val _toggleState = MutableStateFlow(false)
    val toggleState: StateFlow<Boolean> = _toggleState.asStateFlow()

    init {
        viewModelScope.launch {
            getToggleState()
                .stateIn(
                    scope = viewModelScope,
                    initialValue = false,
                    started = SharingStarted.WhileSubscribed(5000),
                )
                .collect(_toggleState::emit)
        }
    }

    fun setToggleState(isChecked: Boolean) {
        viewModelScope.launch {
            setToggle(isChecked)
        }
    }
}
