package io.snabble.sdk.widgets.snabble.stores.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.StartShopping
import io.snabble.sdk.widgets.snabble.stores.viewmodel.StoresViewModel
import org.koin.androidx.compose.getViewModel

@Composable
internal fun StartShoppingWidget(
    modifier: Modifier = Modifier,
    model: StartShoppingItem,
    viewModel: StoresViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    @OptIn(ExperimentalLifecycleComposeApi::class)
    val isCheckedInState = viewModel.isCheckedInFlow.collectAsStateWithLifecycle()
    if (isCheckedInState.value) {
        StartShopping(
            modifier = modifier,
            model = model,
            onAction = onAction,
        )
    }
}

