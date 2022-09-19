package io.snabble.sdk.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.domain.Widget

typealias WidgetClick = (id: String) -> Unit

@Composable
fun DynamicView(
    background: @Composable (() -> Unit),
    widgets: List<Widget>,
    onClick: WidgetClick,
    widgetFactory: WidgetFactory,
) {
    background()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = widgets) { widget ->
            widgetFactory.createWidget(widget, onClick)
        }
    }
}
