package io.snabble.sdk.widgets.snabble.wlan.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import io.snabble.sdk.utils.xx
import io.snabble.sdk.widgets.snabble.wlan.usecases.ConnectToWlanUseCase
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import io.snabble.sdk.widgets.snabble.wlan.usecases.wlanmanager.WlanManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WlanViewModel(
    private val hasWlanConnection: HasWlanConnectionUseCase,
    private val connectToWlanUseCase: ConnectToWlanUseCase,
    private val wifiManager: WlanManager,
) : ViewModel() {

    private val _wifiButtonIsVisible = MutableStateFlow(hasWlanConnection())
    val wifiButtonIsVisible = _wifiButtonIsVisible.asStateFlow()

    fun updateWlanState() {
        _wifiButtonIsVisible.tryEmit(hasWlanConnection())
    }

    fun connect() {
        Log.d("xx", "connecting to Wifi: ")
        wifiManager.connectToWifi("AndroidWifi").message.xx()
    }
}
