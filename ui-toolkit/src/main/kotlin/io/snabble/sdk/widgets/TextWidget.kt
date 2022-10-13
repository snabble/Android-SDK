package io.snabble.sdk.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
fun TextWidget(
    model: TextItem,
    modifier: Modifier = Modifier,
    onAction: OnDynamicAction,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable
            { onAction(DynamicAction(model)) }
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = model.text,
            color = Color(model.textColorSource ?: Color.Black.toArgb()),
            style = when (model.textStyleSource) {
                "body" -> MaterialTheme.typography.bodyLarge
                "footer" -> MaterialTheme.typography.bodySmall
                "title" -> MaterialTheme.typography.headlineMedium
                else -> MaterialTheme.typography.bodyLarge
            }
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
private fun TextWidgetPreview() {
    Column(Modifier.fillMaxSize()) {
        TextWidget(
            model = TextItem(
                id = "1",
                text = "Willkommen bei Snabble",
                textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
                textStyleSource = "title",
                showDisclosure = false,
                padding = Padding(horizontal = 16),
            ),
            onAction = {},
        )
        TextWidget(
            model = TextItem(
                id = "2",
                text = "Scanne deine Produkte und kaufe jetzt ein",
                textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
                textStyleSource = "body",
                showDisclosure = false,
                padding = Padding(horizontal = 16),
            ),
            onAction = {},
        )
    }
}