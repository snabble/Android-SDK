package io.snabble.sdk.dynamicview.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.Theme3Parameters
import com.google.accompanist.themeadapter.material3.createMdc3Theme
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.Padding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding

@Composable
fun ThemeWrapper(content: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = if (!LocalInspectionMode.current) {
        createMdc3Theme(
            context = LocalContext.current,
            layoutDirection = LayoutDirection.Ltr
        )
    } else {
        Theme3Parameters(null, null, null)
    }

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
