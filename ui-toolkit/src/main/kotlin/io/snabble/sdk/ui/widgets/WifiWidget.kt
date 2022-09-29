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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.AppTheme
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectWifiWidget(
    modifier: Modifier = Modifier,
    model: ConnectWifiItem,
    isVisible: Boolean,
    onClick: OnDynamicAction,
) {
    if (isVisible) {
        CompositionLocalProvider(
            // TODO: Providing this app wide?
            LocalRippleTheme provides object : RippleTheme {

                @Composable
                override fun defaultColor(): Color = AppTheme.colors.snabble_primaryColor

                @Composable
                override fun rippleAlpha(): RippleAlpha =
                    RippleTheme.defaultRippleAlpha(Color.Black, lightTheme = !isSystemInDarkTheme())
            }
        ) {
            rememberRipple()
            Card(
                onClick = { onClick(DynamicAction(model)) },
                modifier = Modifier
                    .indication(
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple()
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                        text = "Connect to free Wifi",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight(400),
                        fontSize = 17.sp,
                        letterSpacing = (-0.41).sp

                    )
                    Image(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .wrapContentSize(),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.snabble_wifi),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(AppTheme.colors.snabble_primaryColor)
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun WifiWidgetPreview() {
    ConnectWifiWidget(
        model = ConnectWifiItem(
            id = "wifiii",
            padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
        ),
        isVisible = true,
        onClick = {}
    )
}
