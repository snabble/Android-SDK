package io.snabble.sdk.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toolkit.R


@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun StartShoppingPreview() {
    StartShoppingWidget(
        model = StartShoppingItem("1", 16),
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
        ButtonWidget(
            modifier = modifier,
            model = model,
            text = stringResource(id = R.string.Snabble_Shop_Detail_shopNow),
            onClick = onClick
        )
    }
}