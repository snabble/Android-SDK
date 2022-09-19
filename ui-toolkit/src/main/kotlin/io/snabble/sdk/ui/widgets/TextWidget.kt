package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.Text
import io.snabble.sdk.utils.getComposeColor


@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun TextWidgetPreview() {
    Column(Modifier.fillMaxSize()) {
        TextWidget(
            model = Text(
                id = 1,
                text = "Willkommen bei Snabble",
                textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                textStyleSource = "header",
                showDisclosure = false,
                spacing = 5,
                padding = 16
            )
        )
        TextWidget(
            model = Text(
                id = 1,
                text = "Scanne deine Produkte und kaufe jetzt ein",
                textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                textStyleSource = "body",
                showDisclosure = false,
                spacing = 5,
                padding = 16
            )
        )

    }
}

@Composable
fun TextWidget(
    model: Text,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(model.padding.dp, 0.dp)
    ) {
        Text(
            text = model.text,
            color = Color(model.textColorSource ?: Color.Black.toArgb()),
            style = when (model.textStyleSource) {
                "body" -> MaterialTheme.typography.bodyMedium
                "footer" -> MaterialTheme.typography.bodySmall
                "header" -> MaterialTheme.typography.headlineLarge
                else -> MaterialTheme.typography.bodyMedium
            }
        )
        Spacer(
            modifier = Modifier
                .height(model.spacing.dp)
                .fillMaxWidth()
        )
    }
}



