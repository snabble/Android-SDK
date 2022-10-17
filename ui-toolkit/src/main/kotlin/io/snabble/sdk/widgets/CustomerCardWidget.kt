package io.snabble.sdk.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toInformationItem
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R

@Composable
fun CustomerCardWidget(
    modifier: Modifier = Modifier,
    model: CustomerCardItem,
    onAction: OnDynamicAction,
) {
    InformationWidget(
        modifier = modifier,
        model = (model.toInformationItem()),
        onAction = { onAction(DynamicAction(model)) },
    )
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun CustomerCardPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.dynamicview.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        CustomerCardWidget(
            model = CustomerCardItem(
                id = "an.image",
                text = "Füge deine Kundenkarte hinzu.",
                image = R.drawable.store_logo,
                padding = Padding(start = 8, top = 8, end = 8, bottom = 8),
            ),
            onAction = {}
        )
    }
}
