package io.snabble.sdk.ui.widgets.purchase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
fun OnLifecycleEvent(
    lifecycleEvent: Lifecycle.Event? = null,
    onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit
) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { owner, event ->
            val update = if (lifecycleEvent != null) event == lifecycleEvent else true
            if (update) eventHandler.value(owner, event)
        }

        val lifecycle: Lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose { lifecycle.removeObserver(observer) }
    }
}
