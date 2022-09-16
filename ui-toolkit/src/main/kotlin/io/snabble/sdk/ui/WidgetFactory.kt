package io.snabble.sdk.ui

import io.snabble.sdk.domain.Widget
import io.snabble.sdk.home.WidgetClick

interface WidgetFactory {
    fun createWidget(widget: Widget, click: WidgetClick? = null) {}
}
