package io.snabble.sdk.widgets.snabble.devsettings.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.widgets.snabble.devsettings.login.repositories.DevSettingsLoginRepository
import io.snabble.sdk.data.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class DevSettingsLoginViewModel : ViewModel() {

    private var resetJob: Job? = null

    private val devSettingsLoginRepository: DevSettingsLoginRepository by KoinProvider.inject()

    private val _clickCount = MutableStateFlow(0)
    val clickCount = _clickCount.asStateFlow()

    private var _showError = MutableStateFlow(false)
    internal val showError = _showError.asStateFlow()

    fun incClickCount() {
        resetJob?.cancel()

        val next = clickCount.value.inc()
        _clickCount.tryEmit(next)

        resetJob = viewModelScope.launch {
            delay(300)
            _clickCount.tryEmit(0)
        }
    }

    internal fun onEnableSettingsClick(password: String) {
        viewModelScope.launch {
            val result = devSettingsLoginRepository.enableDevSettings(password)
            if (result !is Success) {
                _showError.tryEmit(true)
            }
        }
    }

    internal fun resetErrorMessage() {
        _showError.tryEmit(false)
    }
}
