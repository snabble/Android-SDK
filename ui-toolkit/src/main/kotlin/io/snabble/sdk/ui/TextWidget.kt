package io.snabble.sdk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.snabble.sdk.domain.Text
import io.snabble.sdk.ui.toolkit.R


@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun TextWidgetPreview() {
    TextWidget(
        model = Text(
            id = 1,
            text = "Hello World",
            textColorSource = R.color.snabble_onboarding_primary,
            textStyleSource = "body",
            showDisclosure = false,
            spacing = 8,
            padding = 16
        )
    )
}


@Composable
fun TextWidget(
    model: Text,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = model.text,
            color = Color(model.textColorSource ?: Color.Black.toArgb()),
            style = when (model.textStyleSource) {
                "body" -> materialBodyLarge
                "footer" -> materialBodyMedium
                "header" -> materialHeadlineMedium
                else -> MaterialTheme.typography.body1

            }
        )
        Spacer(modifier = Modifier
            .height(model.spacing.dp)
            .fillMaxWidth())
    }
}

val materialHeadlineMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontStyle = FontStyle.Normal,
    fontSize = 28.sp,
    letterSpacing = 0.sp,
)

val materialBodyLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontStyle = FontStyle.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.03125.sp,
)

val materialBodyMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontStyle = FontStyle.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.01785714.sp,
)


