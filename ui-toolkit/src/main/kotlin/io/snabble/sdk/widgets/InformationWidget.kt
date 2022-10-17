package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.SnabbleCard

@Composable
fun InformationWidget(
    modifier: Modifier = Modifier,
    model: InformationItem,
    onAction: OnDynamicAction,
) {
    SnabbleCard(
        onClick = { onAction(DynamicAction(model)) },
        modifier = modifier.padding(model.padding.toPaddingValues()),
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = MaterialTheme.padding.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (model.image != null) {
                Image(
                    modifier = Modifier
                        .padding(
                            top = MaterialTheme.padding.large,
                            bottom = MaterialTheme.padding.large,
                            end = MaterialTheme.padding.large
                        ),
                    contentScale = ContentScale.Fit,
                    painter = painterResource(id = model.image),
                    contentDescription = "",
                )
            }
            Text(
                text = model.text,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun InformationWidgetPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.dynamicview.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        InformationWidget(
            model = InformationItem(
                id = "an.image",
                text = "Füge deine Kundenkarte hinzu.",
                image = R.drawable.store_logo,
                padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
            ),
            onAction = {}
        )
    }
}
