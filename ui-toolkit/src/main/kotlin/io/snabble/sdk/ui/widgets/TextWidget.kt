package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.utils.getComposeColor

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun TextWidgetPreview() {
    Column(Modifier.fillMaxSize()) {
        TextWidget(
            model = TextItem(
                id = "1",
                text = "Willkommen bei Snabble",
                textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                textStyleSource = "header",
                showDisclosure = false,
                padding = Padding(start = 16, top = 0, end = 16, bottom = 0),
            )
        )
        TextWidget(
            model = TextItem(
                id = "2",
                text = "Scanne deine Produkte und kaufe jetzt ein",
                textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                textStyleSource = "body",
                showDisclosure = false,
                padding = Padding(start = 16, top = 0, end = 16, bottom = 0),
            )
        )
    }
}

@Composable
fun TextWidget(
    model: TextItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .padding(model.padding.toPaddingValues())
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
    }
}
