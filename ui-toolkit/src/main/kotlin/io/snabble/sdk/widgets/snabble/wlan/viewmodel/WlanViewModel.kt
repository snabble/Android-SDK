package io.snabble.sdk.widgets.snabble.wlan.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.widgets.snabble.wlan.usecases.HasWlanConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WlanViewModel(
    hasWlanConnection: HasWlanConnectionUseCase,
) : ViewModel() {

    private val _wifiButtonIsVisible = MutableStateFlow(hasWlanConnection())
    val wifiButtonIsVisible = _wifiButtonIsVisible.asStateFlow()
}
