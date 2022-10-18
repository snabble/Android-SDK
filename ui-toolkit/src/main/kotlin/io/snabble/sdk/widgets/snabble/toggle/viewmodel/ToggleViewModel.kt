package io.snabble.sdk.widgets.snabble.toggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.toggle.usecases.GetToggleStateUseCase
import io.snabble.sdk.widgets.snabble.toggle.usecases.SaveToggleStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal class ToggleViewModel(
    getToggleState: GetToggleStateUseCase,
    private val saveToggle: SaveToggleStateUseCase,
) : ViewModel() {

    val toggleState = getToggleState()
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(5000),
        )

    fun saveToggleState(isChecked: Boolean) {
        saveToggle(isChecked)
    }
}
