package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.theme.properties.Elevation
import io.snabble.sdk.ui.theme.properties.LocalElevation
import io.snabble.sdk.ui.theme.properties.LocalPadding
import io.snabble.sdk.ui.theme.properties.applyElevation
import io.snabble.sdk.ui.theme.properties.applyPadding
import io.snabble.sdk.ui.theme.properties.elevation
import io.snabble.sdk.ui.theme.properties.padding
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun WifiWidgetPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.ui.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        ConnectWifiWidget(
            model = ConnectWifiItem(
                id = "wifiii",
                padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
            ),
            isVisible = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectWifiWidget(
    modifier: Modifier = Modifier,
    model: ConnectWifiItem,
    isVisible: Boolean,
    onclick: WidgetClick = {}
) {
    if (isVisible) {
        CompositionLocalProvider(
            // TODO: Providing this app wide?
            LocalRippleTheme provides object : RippleTheme {

                @Composable
                override fun defaultColor(): Color = MaterialTheme.colorScheme.primary

                @Composable
                override fun rippleAlpha(): RippleAlpha =
                    RippleTheme.defaultRippleAlpha(Color.Black, lightTheme = !isSystemInDarkTheme())
            }
        ) {
            rememberRipple()
            Card(
                onClick = { onclick },
                modifier = Modifier
                    .indication(
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple()
                    ),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.small),
            ) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(model.padding.toPaddingValues()),
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
    }
}