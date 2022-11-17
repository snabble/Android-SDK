package io.snabble.sdk.widgets.snabble.wlan.viewmodel

import android.util.Log
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

    init {
        viewModelScope.launch {
            val value = hasWlanConnectionUseCase("AndroidWifi")
            _wifiButtonIsVisible.tryEmit(value)
        }
    }

    fun updateWlanState() {
        viewModelScope.launch {
            val value = hasWlanConnectionUseCase("AndroidWifi")
            _wifiButtonIsVisible.tryEmit(value)
        }
    }

    fun connect() {
        Log.d("xx", "connecting to Wifi: ")
        wifiManager.connectToWifi("AndroidWifi").message
    }
}
