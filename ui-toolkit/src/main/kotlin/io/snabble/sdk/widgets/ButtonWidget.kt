package io.snabble.sdk.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.Widget
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString

@Composable
fun ButtonWidget(
    modifier: Modifier = Modifier,
    model: ButtonItem,
    onAction: OnDynamicAction = {},
) {
    Button(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues())
            .then(modifier),
        onClick = { onAction(DynamicAction(model)) },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(
                model.backgroundColorSource ?: MaterialTheme.colorScheme.primary.toArgb()
            ),
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = model.text,
            style = MaterialTheme.typography.labelLarge,
            color = Color(model.foregroundColorSource ?: MaterialTheme.colorScheme.onPrimary.toArgb())
        )
    }
}

@Composable
fun ButtonWidget(
    modifier: Modifier = Modifier,
    widget: Widget,
    padding: Padding,
    text: String,
    onAction: OnDynamicAction = {},
) {
    Button(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .padding(padding.toPaddingValues())
            .then(modifier),
        onClick = { onAction(DynamicAction(widget)) },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun ButtonPreview() {
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
