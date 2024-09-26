package io.snabble.sdk.widgets.snabble

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material.ripple
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.snabble.sdk.dynamicview.theme.properties.elevation

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SnabbleCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val rippleConfiguration = RippleConfiguration(
        color = MaterialTheme.colorScheme.primary,
        rippleAlpha = RippleDefaults.rippleAlpha(Color.Black, !isSystemInDarkTheme())
    )

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .indication(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                )
                .then(modifier),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.small),
        ) {
            content()
        }
    }
}
