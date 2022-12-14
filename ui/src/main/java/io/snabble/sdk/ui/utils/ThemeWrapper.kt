package io.snabble.sdk.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme

@Composable
internal fun ThemeWrapper(content: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = createMdc3Theme(
        context = LocalContext.current,
        layoutDirection = LayoutDirection.Ltr
    )
    CompositionLocalProvider {
        MaterialTheme(
            colorScheme = colorScheme ?: MaterialTheme.colorScheme,
            typography = typography ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes,
        ) {
            content()
        }
    }
}
