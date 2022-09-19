package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Button
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.utils.dp
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun ButtonPreview() {
    ButtonWidget(
        model = Button(
            id = "a.button",
            text = LocalContext.current
                .getResourceString("Sample_Dashboard_button")
                .toString(),
            foregroundColorSource = null,
            backgroundColorSource = LocalContext.current
                .getComposeColor("snabble_onboarding_primary"),
            spacing = 8,
            padding = 8,
        )
    )
}

@Composable
fun ButtonWidget(
    model: Button,
    modifier: Modifier = Modifier,
    onClick: WidgetClick = {},
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { onClick(model.id) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(
                    model.backgroundColorSource ?: MaterialTheme.colors.primary.toArgb()
                ),
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.align(Center),
        ) {
            Text(
                text = model.text,
                color = Color(
                    model.foregroundColorSource ?: MaterialTheme.colors.onPrimary.toArgb()
                )
            )
        }
    }
}
