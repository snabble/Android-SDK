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
    header: @Composable (() -> Unit),
    widgets: List<Widget>,
    widgetFactory: WidgetFactory,
    onClick: WidgetClick? = null,
) {
    header()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = widgets) { widget ->
            widgetFactory.createWidget(widget, onClick)
        }
    }
}
