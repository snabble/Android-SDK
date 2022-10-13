package io.snabble.sdk.widgets.snabble.wlan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.ConnectWifiItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.ConnectWlanWidget
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WifiViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun ConnectWifiWidget(
    modifier: Modifier = Modifier,
    model: ConnectWifiItem,
    viewModel: WifiViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    val isButtonVisibleState = viewModel.wifiButtonIsVisible.collectAsStateWithLifecycle()
    if (isButtonVisibleState.value) {
        ConnectWlanWidget(
            modifier = modifier,
            model = model,
            onAction = onAction
        )
    }
}
