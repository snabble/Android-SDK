package io.snabble.sdk.ui

import androidx.lifecycle.ViewModel
import io.snabble.sdk.domain.Widget
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DynamicViewModel(
// TODO: ???
) : ViewModel() {

    private val _actions = MutableSharedFlow<DynamicAction>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val actions: SharedFlow<DynamicAction> = _actions.asSharedFlow()

    internal fun sendAction(action: DynamicAction) {
        _actions.tryEmit(action)
    }
}

data class DynamicAction(
    val widget: Widget,
    val info: Map<String, Any>? = null
)
