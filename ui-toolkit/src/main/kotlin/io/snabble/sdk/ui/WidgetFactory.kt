package io.snabble.sdk.ui

import io.snabble.sdk.domain.Widget

interface WidgetFactory {
    fun createWidget(widget: Widget, click: WidgetClick? = null) {}
}
