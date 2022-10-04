package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString


@Composable
fun ButtonWidget(
    modifier: Modifier = Modifier,
    model: ButtonItem,
    onClick: OnDynamicAction = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues())
    ) {
        Button(
            onClick = { onClick(DynamicAction(model)) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(
                    model.backgroundColorSource ?: MaterialTheme.colorScheme.primary.toArgb()
                ),
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = model.text,
                style = MaterialTheme.typography.labelLarge,
                color = Color(
                    model.foregroundColorSource ?: MaterialTheme.colorScheme.onPrimary.toArgb()
                )
            )
        }
    }
}

@Composable
fun ButtonWidget(
    modifier: Modifier = Modifier,
    widget: Widget,
    padding: Padding,
    text: String,
    onClick: OnDynamicAction = {},
) {
    Box(
        modifier = modifier
            .padding(padding.toPaddingValues())
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onClick(DynamicAction(widget)) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge, //TODO: Evaluate right typo
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun ButtonPreview() {
    ButtonWidget(
        model = ButtonItem(
            id = "a.button",
            text = LocalContext.current
                .getResourceString("Sample_Dashboard_button")
                .toString(),
            foregroundColorSource = null,
            backgroundColorSource = LocalContext.current
                .getComposeColor("snabble_onboarding_primary"),
            padding = Padding(horizontal = 8),
        )
    )
}
