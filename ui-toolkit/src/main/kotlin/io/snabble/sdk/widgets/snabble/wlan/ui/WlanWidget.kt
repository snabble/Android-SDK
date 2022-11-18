package io.snabble.sdk.widgets.snabble.wlan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.ConnectWlanWidget
import io.snabble.sdk.widgets.snabble.purchase.OnLifecycleEvent
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WlanViewModel
import org.koin.core.component.get

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun ConnectWlanWidget(
    modifier: Modifier = Modifier,
    model: ConnectWlanItem,
    viewModel: WlanViewModel = viewModelScoped { KoinProvider.get() },
    onAction: OnDynamicAction,
) {
    val isButtonVisibleState = viewModel.wifiButtonIsVisible.collectAsStateWithLifecycle()
    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) { _, _ ->
        viewModel.updateWlanState(model.ssid)
    }

    if (isButtonVisibleState.value) {
        ConnectWlanWidget(
            modifier = modifier,
            model = model,
            onAction = {
                viewModel.connect(model.ssid)
                onAction(it)
            }
        )
    }
}
