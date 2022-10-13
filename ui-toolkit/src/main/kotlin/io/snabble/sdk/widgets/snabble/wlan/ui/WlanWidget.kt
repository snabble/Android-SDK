package io.snabble.sdk.widgets.snabble.wlan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.ConnectWlanWidget
import io.snabble.sdk.widgets.snabble.wlan.viewmodel.WlanViewModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun ConnectWlanWidget(
    modifier: Modifier = Modifier,
    model: ConnectWlanItem,
    viewModel: WlanViewModel = viewModelScoped { KoinProvider.scope.get() },
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
