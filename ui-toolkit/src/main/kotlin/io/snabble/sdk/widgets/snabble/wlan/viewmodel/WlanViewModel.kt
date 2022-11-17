package io.snabble.sdk.widgets.snabble.wlan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import io.snabble.sdk.wlanmanager.WlanManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WlanViewModel(
    private val hasWlanConnectionUseCase: HasWlanConnectionUseCase,
    private val wifiManager: WlanManager,
) : ViewModel() {

    private val _wifiButtonIsVisible = MutableStateFlow(false)
    val wifiButtonIsVisible = _wifiButtonIsVisible.asStateFlow()

    fun updateWlanState() {
        viewModelScope.launch {
            val value = hasWlanConnectionUseCase("AndroidWifi")
            _wifiButtonIsVisible.tryEmit(value)
        }
    }

    fun connect() {
        val status = wifiManager.connectToWifi("AndroidWifi")
        when (status) {
            is Success -> _wifiButtonIsVisible.tryEmit(false)
            is Error -> _wifiButtonIsVisible.tryEmit(false)
        }
    }
}
