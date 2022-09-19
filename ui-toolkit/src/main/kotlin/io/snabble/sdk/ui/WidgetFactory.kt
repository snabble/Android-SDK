package io.snabble.sdk.ui

import androidx.compose.runtime.Composable
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Text
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.widgets.TextWidget

interface WidgetFactory {

    @Composable
    fun createWidget(widget: Widget, click: WidgetClick)

}

class WidgetFactoryImpl : WidgetFactory {

    @Composable
    override fun createWidget(widget: Widget, click: WidgetClick) = when (widget) {
        is Text -> TextWidget(model = widget)
        is Image -> ImageWidget(model = widget)
        else -> {}
    }
}
