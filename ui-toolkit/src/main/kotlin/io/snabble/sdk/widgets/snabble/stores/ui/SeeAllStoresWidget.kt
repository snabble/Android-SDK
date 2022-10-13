package io.snabble.sdk.widgets.snabble.stores.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.SeeAllStoresWidget
import io.snabble.sdk.widgets.snabble.stores.viewmodel.StoresViewModel
import org.koin.androidx.compose.getViewModel

@Composable
internal fun SeeAllStoresWidget(
    modifier: Modifier = Modifier,
    model: SeeAllStoresItem,
    viewModel: StoresViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        @OptIn(ExperimentalLifecycleComposeApi::class)
        val isCheckedInState = viewModel.isCheckedInFlow.collectAsStateWithLifecycle()
        SeeAllStoresWidget(
            modifier = modifier,
            model = model,
            isChecked = isCheckedInState.value,
            onAction = onAction
        )
    }
}
