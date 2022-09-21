package io.snabble.sdk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Immutable
data class CustomColors(
    val snabble_primaryColor: Color,
    val snabble_colorOnPrimary: Color,
    val snabble_textColorLight: Color,
    val snabble_textColorDark: Color,
    val snabble_colorSurface: Color,
    val snabble_colorOnSurface: Color

)

@Immutable
data class CustomTypography(
    val body: TextStyle,
    val footer: TextStyle,
    val header: TextStyle
)

@Immutable
data class CustomElevation(
    val primary: Dp,
    val secondary: Dp
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        snabble_primaryColor = Color(0xFF0077bb),
        snabble_colorOnPrimary = Color(0xFFFFFFFF),
        snabble_textColorLight = Color (0xFFFFFFFF),
        snabble_textColorDark = Color(0xFF1D1F24),
        snabble_colorSurface = Color(0xFFEAF0F8),
        snabble_colorOnSurface = Color(0xFF000000)
    )
}
val LocalCustomTypography = staticCompositionLocalOf {
    CustomTypography(
        body = TextStyle.Default,
        footer = TextStyle.Default,
        header = TextStyle.Default
    )
}
val LocalCustomElevation = staticCompositionLocalOf {
    CustomElevation(
        primary = Dp.Unspecified,
        secondary = Dp.Unspecified
    )
}

// Use with eg. CustomTheme.elevation.small
object AppTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
    val typography: CustomTypography
        @Composable
        get() = LocalCustomTypography.current
    val elevation: CustomElevation
        @Composable
        get() = LocalCustomElevation.current
}
