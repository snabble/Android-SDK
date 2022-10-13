package io.snabble.sdk.widgets.snabble.onboardingtoggle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.utils.xx
import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingToggleViewModel() : ViewModel() {

    private val _toggleState = MutableStateFlow(false)
    val toggleState: StateFlow<Boolean> = _toggleState.asStateFlow()

    private val repo: ToggleRepository by KoinProvider.getKoin().inject()

    init {
        viewModelScope.launch {
            repo.xx("Flow for Onboarding").getToggleState(KEY_SHOW_ONBOARDING).collect(_toggleState::emit)
        }
    }

    fun setToggleState(id: String, isChecked: Boolean) {
        viewModelScope.launch {
            repo.saveToggleState(id, isChecked)
        }
    }

    private companion object {

        const val KEY_SHOW_ONBOARDING = "show_onboarding"
    }
}
