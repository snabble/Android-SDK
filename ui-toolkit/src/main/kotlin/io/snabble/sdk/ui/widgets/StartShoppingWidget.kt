package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toolkit.R

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun StartShoppingPreview() {
    StartShoppingWidget(
        model = StartShoppingItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        checkinState = true
    )
}

@Composable
fun StartShoppingWidget(
    modifier: Modifier = Modifier,
    model: StartShoppingItem,
    checkinState: Boolean = false,
    onClick: WidgetClick = {},
) {
    if (checkinState) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.align(Alignment.Center),
                widget = model,
                text = stringResource(id = R.string.Snabble_Shop_Detail_shopNow),
                onClick = onClick
            )
        }
    }
}
