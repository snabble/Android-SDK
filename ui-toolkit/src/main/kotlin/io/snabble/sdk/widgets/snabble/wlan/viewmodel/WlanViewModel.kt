package io.snabble.sdk.widgets.snabble.wlan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import io.snabble.sdk.wlanmanager.WlanManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WlanViewModel(
    private val hasWlanConnectionUseCase: HasWlanConnectionUseCase,
    private val wifiManager: WlanManager,
) : ViewModel() {

    private val _wifiButtonIsVisible = MutableStateFlow(false)
    val wifiButtonIsVisible = _wifiButtonIsVisible.asStateFlow()

    fun updateWlanState(ssid: String?) {
        if (ssid == null) {
            _wifiButtonIsVisible.tryEmit(false)
            return
        }

        viewModelScope.launch {
            val value = hasWlanConnectionUseCase(ssid)
            _wifiButtonIsVisible.tryEmit(value)
        }
    }

    fun connect(ssid: String?) {
        ssid ?: return
        wifiManager.connectToWlan(ssid)
        _wifiButtonIsVisible.tryEmit(false)
    }
}
