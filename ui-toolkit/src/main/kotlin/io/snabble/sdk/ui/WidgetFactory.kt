package io.snabble.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.domain.Text as TextWidget

interface WidgetFactory {

    @Composable
    fun createWidget(widget: Widget, click: WidgetClick): @Composable () -> Unit
}

class WidgetFactoryImpl : WidgetFactory {

    @Composable
    override fun createWidget(widget: Widget, click: WidgetClick): @Composable () -> Unit {
        return when (widget) {
            is TextWidget -> {
                {
                    Text(
                        modifier = Modifier
                            .clickable { click("${widget.id}") },
                        text = widget.text,
                    )
                }
            }
            else -> {
                {}
            }
        }
    }
}
