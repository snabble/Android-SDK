package io.snabble.sdk.dynamicview.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.Widget
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

open class DynamicViewModel : ViewModel() {

    private val _dynamicConfig = MutableStateFlow<DynamicConfig?>(null)
    val dynamicConfig = _dynamicConfig.asStateFlow()

    fun setConfig(config: DynamicConfig?) {
        _dynamicConfig.tryEmit(config)
    }

    private val _actions = MutableSharedFlow<DynamicAction>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val actions = _actions.asSharedFlow()

    internal fun sendAction(action: DynamicAction) {
        _actions.tryEmit(action)
    }
}

data class DynamicAction(
    val widget: Widget,
    val info: Map<String, Any>? = null,
)
