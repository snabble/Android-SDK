package io.snabble.sdk.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp

val Typography = Typography(

    h1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontStyle = FontStyle.Normal,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
    ),

    body1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontStyle = FontStyle.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.03125.sp,
    ),

    body2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontStyle = FontStyle.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.01785714.sp,
    )
)