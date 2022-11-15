package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.SnabbleCard

@Composable
fun ConnectWlanWidget(
    modifier: Modifier = Modifier,
    model: ConnectWlanItem,
    onAction: OnDynamicAction,
) {
    SnabbleCard(
        onClick = { onAction(DynamicAction(model)) },
        modifier = modifier.padding(model.padding.toPaddingValues())
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = MaterialTheme.padding.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                text = "Connect to free Wifi",
                style = MaterialTheme.typography.bodyMedium
            )
            Image(
                modifier = Modifier
                    .padding(start = MaterialTheme.padding.large)
                    .wrapContentSize(),
                contentScale = ContentScale.Fit,
                painter = painterResource(id = R.drawable.snabble_wifi),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun WlanWidgetPreview() {
    ThemeWrapper {
        ConnectWlanWidget(
            model = ConnectWlanItem(
                id = "wifi",
                padding = Padding(horizontal = 16, vertical = 8),
            ),
            onAction = {}
        )
    }
}
