package io.snabble.sdk.ui

import androidx.compose.runtime.Composable
import io.snabble.sdk.domain.Text
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.widgets.TextWidget

interface WidgetFactory {

    @Composable
    fun createWidget(widget: Widget, click: WidgetClick): @Composable () -> Unit
}

class WidgetFactoryImpl : WidgetFactory {

    @Composable
    override fun createWidget(widget: Widget, click: WidgetClick): @Composable () -> Unit {
        return when (widget) {
            is Text -> {
                {
                    TextWidget(
                        model = widget
                    )
                }
            }
            else -> {
                {}
            }
        }
    }
}
