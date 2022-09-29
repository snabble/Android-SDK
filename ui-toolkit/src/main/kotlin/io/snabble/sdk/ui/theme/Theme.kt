package io.snabble.sdk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.material.composethemeadapter3.createMdc3Theme
import io.snabble.sdk.ui.theme.properties.Elevation
import io.snabble.sdk.ui.theme.properties.LocalElevation
import io.snabble.sdk.ui.theme.properties.LocalPadding
import io.snabble.sdk.ui.theme.properties.Padding
import io.snabble.sdk.ui.theme.properties.applyElevation
import io.snabble.sdk.ui.theme.properties.applyPadding

@Composable
fun ThemeWrapper(content: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = createMdc3Theme(
        context = LocalContext.current,
        layoutDirection = LayoutDirection.Ltr
    )
    CompositionLocalProvider(
        LocalElevation provides Elevation().applyElevation(),
        LocalPadding provides Padding().applyPadding()
    ) {
        MaterialTheme(
            colorScheme = colorScheme ?: MaterialTheme.colorScheme,
            typography = typography ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes,
        ) {
            content()
        }
    }
}