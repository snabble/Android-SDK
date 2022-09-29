package io.snabble.sdk.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toInformationItem
import io.snabble.sdk.ui.toolkit.R

@Composable
fun CustomerCardWidget(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    model: CustomerCardItem,
    onClick: OnDynamicAction,
) {
    if (isVisible) {
        InformationWidget(
            model = (model.toInformationItem()),
            onClick = { onClick(DynamicAction(model)) },
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun CustomerCardWidgetPreview() {
    CustomerCardWidget(
        model = CustomerCardItem(
            id = "an.image",
            text = "Füge deine Kundenkarte hinzu.",
            imageSource = R.drawable.store_logo,
            padding = Padding(start = 8, top = 8, end = 8, bottom = 8),
        ),
        isVisible = true,
        onClick = {}
    )
}
