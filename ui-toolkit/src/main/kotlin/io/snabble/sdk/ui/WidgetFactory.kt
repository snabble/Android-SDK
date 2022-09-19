package io.snabble.sdk.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Text
import io.snabble.sdk.domain.Widget

interface WidgetFactory {

    @Composable
    fun createWidget(widget: Widget, click: WidgetClick)

}

class WidgetFactoryImpl : WidgetFactory {

    @Composable
    override fun createWidget(widget: Widget, click: WidgetClick) = when (widget) {
        is Text -> Text(text = widget.text)
        is Image -> ImageWidget(model = widget)
        else -> {}
    }
}
