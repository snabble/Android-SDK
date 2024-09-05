package io.snabble.sdk.ui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.themeadapter.material3.createMdc3Theme
import io.snabble.sdk.Snabble
import io.snabble.sdk.remoteTheme.AppTheme
import io.snabble.sdk.ui.remotetheme.asColor

@Composable
internal fun ThemeWrapper(content: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = createMdc3Theme(
        context = LocalContext.current,
        layoutDirection = LayoutDirection.Ltr
    )

    var currentColorScheme by remember { mutableStateOf(colorScheme) }

    val currentTheme = Snabble.checkedInProject
        .asFlow()
        .collectAsStateWithLifecycle(initialValue = Snabble.checkedInProject.value)
        .value
        ?.appTheme

    currentColorScheme = when (currentTheme) {
        null -> colorScheme
        else -> colorScheme?.applyTheme(appTheme = currentTheme)
    }
    CompositionLocalProvider {
        MaterialTheme(
            colorScheme = currentColorScheme ?: MaterialTheme.colorScheme,
            typography = typography ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes,
        ) {
            content()
        }
    }
}

private fun String.asComposeColor(): Color = Color(asColor())

@Composable
private fun ColorScheme.applyTheme(appTheme: AppTheme): ColorScheme = when (isSystemInDarkTheme()) {
    true -> {
        copy(
            primary = appTheme.darkModeColors?.primaryColor?.asComposeColor()
                ?: appTheme.lightModeColors?.primaryColor?.asComposeColor() ?: primary,
            onPrimary = appTheme.darkModeColors?.onPrimaryColor?.asComposeColor()
                ?: appTheme.lightModeColors?.onPrimaryColor?.asComposeColor() ?: onPrimary,
            secondary = appTheme.darkModeColors?.secondaryColor?.asComposeColor()
                ?: appTheme.lightModeColors?.secondaryColor?.asComposeColor() ?: secondary,
            onSecondary = appTheme.darkModeColors?.onSecondaryColor?.asComposeColor()
                ?: appTheme.lightModeColors?.onSecondaryColor?.asComposeColor() ?: onSecondary
        )
    }

    false -> copy(
        primary = appTheme.lightModeColors?.primaryColor?.asComposeColor() ?: primary,
        onPrimary = appTheme.lightModeColors?.onPrimaryColor?.asComposeColor() ?: onPrimary,
        secondary = appTheme.lightModeColors?.secondaryColor?.asComposeColor() ?: secondary,
        onSecondary = appTheme.lightModeColors?.onSecondaryColor?.asComposeColor() ?: onSecondary
    )
}
