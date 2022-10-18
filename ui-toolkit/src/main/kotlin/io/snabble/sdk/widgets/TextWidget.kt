package io.snabble.sdk.widgets

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.utils.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
fun TextWidget(
    model: TextItem,
    modifier: Modifier = Modifier,
    onAction: OnDynamicAction,
    indication: Indication? = rememberRipple(),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = indication,
            ) {
                onAction(DynamicAction(model))
            }
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = model.text,
            color = Color(model.textColor ?: Color.Black.toArgb()),
            style = when (model.textStyle) {
                "title" -> MaterialTheme.typography.headlineLarge
                "body" -> MaterialTheme.typography.bodyLarge
                "footer" -> MaterialTheme.typography.bodyMedium
                else -> MaterialTheme.typography.bodyLarge
            }
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun TextWidgetPreview() {
    Column(Modifier.fillMaxWidth()) {
        TextWidget(
            model = TextItem(
                id = "1",
                text = "Willkommen bei Snabble",
                textColor = MaterialTheme.colorScheme.primary.toArgb(),
                textStyle = "title",
                showDisclosure = false,
                padding = Padding(horizontal = 16),
            ),
            onAction = {},
        )
        TextWidget(
            model = TextItem(
                id = "2",
                text = "Scanne deine Produkte und kaufe jetzt ein",
                textColor = MaterialTheme.colorScheme.primary.toArgb(),
                textStyle = "body",
                showDisclosure = false,
                padding = Padding(horizontal = 16),
            ),
            onAction = {},
        )
    }
}
