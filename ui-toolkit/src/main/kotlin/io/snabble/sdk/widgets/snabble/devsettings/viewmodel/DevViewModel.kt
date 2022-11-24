package io.snabble.sdk.widgets.snabble.devsettings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.utils.xx
import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepository
import io.snabble.sdk.wlanmanager.data.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class DevViewModel(

) : ViewModel() {

    private var resetJob: Job? = null

    private val devSettingsRepository: DevSettingsRepository by KoinProvider.inject()

    private val _clickCount = MutableStateFlow(0)
    val clickCount = _clickCount.asStateFlow()

    private var _showError = MutableStateFlow(false)
    val showError = _showError.asStateFlow()

    fun onEnableSettingsClick(password: String) {
        viewModelScope.launch {
            val result = devSettingsRepository.enableDevSettings(password)
            if (result !is Success) {
                _showError.tryEmit(true)
            }
        }
    }

    fun incClickCount() {
        resetJob?.cancel()
        val next = clickCount.value.inc()
        _clickCount.tryEmit(next.xx("Emitted: "))

        resetJob = viewModelScope.launch {
            delay(300)
            _clickCount.tryEmit(0.xx("Reset: "))
        }
    }

    internal fun resetErrorMessage() {
        _showError.tryEmit(false)
    }
}
