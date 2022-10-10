package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.OnDynamicAction
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding
import io.snabble.sdk.dynamicview.theme.properties.elevation
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationWidget(
    modifier: Modifier = Modifier,
    model: InformationItem,
    onAction: OnDynamicAction
) {
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
            onClick = { onAction(DynamicAction(model)) },
            modifier = Modifier
                .padding(model.padding.toPaddingValues())
                .indication(
                    interactionSource = MutableInteractionSource(),
                    indication = rememberRipple()
                )
                .then(modifier),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.small),
        ) {
            Row(
                modifier = modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.imageSource != null) {
                    Image(
                        modifier = Modifier
                            .padding(
                                top = MaterialTheme.padding.large,
                                bottom = MaterialTheme.padding.large,
                                end = MaterialTheme.padding.large
                            ),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = model.imageSource),
                        contentDescription = "",
                    )
                }
                Text(
                    text = model.text,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun InformationWidgetPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.dynamicview.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        InformationWidget(
            model = InformationItem(
                id = "an.image",
                text = "Füge deine Kundenkarte hinzu.",
                imageSource = R.drawable.store_logo,
                padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
            ),
            onAction = {}
        )
    }
}
