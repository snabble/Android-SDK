package io.snabble.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Text
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.widgets.ImageWidget

typealias WidgetClick = (id: String) -> Unit

@Composable
fun DynamicView(
    background: @Composable (() -> Unit),
    widgets: List<Widget>,
    onClick: WidgetClick,
) {
    background()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = widgets) { widget ->
            Widget(widget = widget, onClick)
        }
    }
}

@Composable
fun Widget(widget: Widget, click: WidgetClick) = when (widget) {
    is Text -> {
        Text(
            text = widget.text,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    is Image -> {
        ImageWidget(
            model = widget,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    else -> {}
}
