package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.AppTheme
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R

@Composable
fun SeeAllStoresWidget(
    modifier: Modifier = Modifier,
    model: SeeAllStoresItem,
    checkInState: Boolean,
    onClick: OnDynamicAction = {},
) {

    Box(modifier = modifier.fillMaxWidth()) {
        if (!checkInState) {
            ButtonWidget(
                modifier = modifier.align(Alignment.Center),
                widget = model,
                text = stringResource(id = R.string.Snabble_DynamicStack_Shop_show),
                onClick = onClick
            )
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
                    .height(48.dp)
                    .padding(model.padding.toPaddingValues())
                    .clickable { onClick(DynamicAction(model)) },
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(id = R.string.Snabble_DynamicStack_Shop_show),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun SeeAllStoresPreview() {
    Column(Modifier.fillMaxSize()) {
        SeeAllStoresWidget(
            model = SeeAllStoresItem(
                id = "1",
                padding = Padding(horizontal = 16, vertical = 5)
            ),
            checkInState = true
        )
        SeeAllStoresWidget(
            model = SeeAllStoresItem(
                id = "1",
                padding = Padding(horizontal = 16, vertical = 5)
            ),
            checkInState = false
        )
    }
}