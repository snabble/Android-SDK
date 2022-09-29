package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.AppTheme
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationWidget(
    modifier: Modifier = Modifier,
    model: InformationItem,
    onClick: OnDynamicAction
) {
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
                .padding(model.padding.toPaddingValues())
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
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.imageSource != null) {
                    Image(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = model.imageSource),
                        contentDescription = "",
                    )
                }
                Text(
                    text = model.text,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight(400),
                    fontSize = 17.sp,
                    letterSpacing = (-0.41).sp
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun InformationWidgetPreview() {
    InformationWidget(
        model = InformationItem(
            id = "an.image",
            text = "Füge deine Kundenkarte hinzu.",
            imageSource = R.drawable.store_logo,
            padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
        ),
        onClick = {}
    )
}
