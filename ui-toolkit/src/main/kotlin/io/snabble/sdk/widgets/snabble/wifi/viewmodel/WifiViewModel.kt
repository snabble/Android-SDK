package io.snabble.sdk.widgets.snabble.wifi.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.widgets.snabble.wifi.domain.GetAvailableWifiUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WifiViewModel internal constructor(
    getAvailableWifiUseCase: GetAvailableWifiUseCase
) : ViewModel() {

    private val _wifiButtonIsVisible = MutableStateFlow(getAvailableWifiUseCase())
    val wifiButtonIsVisible: StateFlow<Boolean> = _wifiButtonIsVisible

}
