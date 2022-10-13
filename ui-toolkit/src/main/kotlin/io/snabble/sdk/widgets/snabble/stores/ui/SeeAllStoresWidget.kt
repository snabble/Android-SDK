package io.snabble.sdk.widgets.snabble.stores.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.ButtonWidget
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
        SeeAllStores(
            modifier = modifier,
            model = model,
            isChecked = isCheckedInState.value,
            onAction = onAction
        )
    }
}

@Composable
fun SeeAllStores(
    modifier: Modifier = Modifier,
    model: SeeAllStoresItem,
    isChecked: Boolean,
    onAction: OnDynamicAction,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (!isChecked) {
            ButtonWidget(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(modifier),
                widget = model,
                padding = model.padding,
                text = stringResource(id = R.string.Snabble_DynamicView_Shop_show),
                onAction = onAction
            )
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
                    .height(48.dp)
                    .padding(model.padding.toPaddingValues())
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { onAction(DynamicAction(model)) }
                    .then(modifier),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(id = R.string.Snabble_DynamicView_Shop_show),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun SeeAllStoresCheckedInPreview() {
    SeeAllStores(
        model = SeeAllStoresItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        isChecked = true,
        onAction = {},
    )
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun SeeAllStoresNotCheckedInPreview() {
    SeeAllStores(
        model = SeeAllStoresItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        isChecked = false,
        onAction = {},
    )
}