package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.ConnectWifiItem
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
fun ConnectWlanWidget(
    modifier: Modifier = Modifier,
    model: ConnectWifiItem,
    onAction: OnDynamicAction,
) {
    SnabbleCard(
        onClick = { onAction(DynamicAction(model)) },
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(model.padding.toPaddingValues())
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
private fun WifiWidgetPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.dynamicview.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        ConnectWlanWidget(
            model = ConnectWifiItem(
                id = "wifi",
                padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
            ),
            onAction = {}
        )
    }
}
