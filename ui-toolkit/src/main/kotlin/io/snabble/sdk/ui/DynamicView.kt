package io.snabble.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.widgets.ButtonWidget
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.ui.widgets.Purchase
import io.snabble.sdk.ui.widgets.PurchasesWidget
import io.snabble.sdk.ui.widgets.TextWidget

typealias WidgetClick = (id: String) -> Unit

@Composable
fun DynamicView(
    modifier: Modifier = Modifier,
    background: @Composable (() -> Unit),
    widgets: List<Widget>,
    onClick: WidgetClick,
) {
    Box(
        modifier = modifier
    ) {
        background()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(items = widgets) { widget ->
                Widget(widget = widget, onClick)
            }
        }
    }
}

@Composable
fun Widget(widget: Widget, click: WidgetClick) = when (widget) {
    is TextItem -> {
        TextWidget(
            model = widget,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    is ImageItem -> {
        ImageWidget(
            model = widget,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    is ButtonItem -> {
        ButtonWidget(
            model = widget,
            onClick = click,
        )
    }
    is PurchasesItem -> {
        PurchasesWidget(
            model = widget,
            // TODO: State!!
            purchases = listOf(
                Purchase(amount = "13,37 €", title = "Snabble Store Bonn", time = "Today"),
                Purchase(
                    amount = "7,56 €",
                    title = "Snabble Store Bonn Dransdorf",
                    time = "Yesterday"
                ),
                Purchase(
                    amount = "42,08 €",
                    title = "Snabble Store Bonn Bad Godesberg",
                    time = "Two days Ago"
                ),
                Purchase(amount = "156,87 €", title = "Snabble Store Koblenz", time = "Last week"),
                Purchase(amount = "20,01 €", title = "Snabble Store London", time = "Last month"),
            )
        )
    }
    else -> {}
}
