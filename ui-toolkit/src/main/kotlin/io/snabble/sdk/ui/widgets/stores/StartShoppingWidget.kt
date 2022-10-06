package io.snabble.sdk.ui.widgets.stores

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.ButtonWidget
import io.snabble.sdk.ui.widgets.stores.viewmodel.StoresViewModel
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
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.align(Alignment.Center),
                widget = model,
                padding = model.padding,
                text = stringResource(id = R.string.Snabble_Shop_Detail_shopNow),
                onAction = onAction
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun StartShoppingPreview() {
    StartShoppingWidget(
        model = StartShoppingItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        onAction = {}
    )
}
