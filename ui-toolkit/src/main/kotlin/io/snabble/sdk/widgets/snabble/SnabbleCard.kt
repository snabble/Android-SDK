package io.snabble.sdk.widgets.snabble

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.snabble.sdk.dynamicview.theme.properties.elevation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnabbleCard(
    modifier: Modifier,
    padding: PaddingValues,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleTheme provides object : RippleTheme {

            @Composable
            override fun defaultColor(): Color = MaterialTheme.colorScheme.primary

            @Composable
            override fun rippleAlpha(): RippleAlpha =
                RippleTheme.defaultRippleAlpha(Color.Black, lightTheme = !isSystemInDarkTheme())
        }
    ) {
        Card(
            modifier = Modifier

                .indication(
                    interactionSource = MutableInteractionSource(),
                    indication = rememberRipple()
                )
                .then(modifier)
                .padding(padding),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.small),
            onClick = onClick,
        ) {
            content()
        }
    }

}