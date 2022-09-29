package io.snabble.sdk.ui.widgets.stores

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetAvailableWifiUseCase

class WifiViewModel internal constructor(
    getAvailableWifiUseCase: GetAvailableWifiUseCase
) : ViewModel() {

    var wifiState = getAvailableWifiUseCase()

}
