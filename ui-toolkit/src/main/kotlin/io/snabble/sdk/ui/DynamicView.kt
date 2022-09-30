package io.snabble.sdk.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.widgets.ButtonWidget
import io.snabble.sdk.ui.widgets.ConnectWifiWidget
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.ui.widgets.InformationWidget
import io.snabble.sdk.ui.widgets.LocationPermissionWidget
import io.snabble.sdk.ui.widgets.SeeAllStoresWidget
import io.snabble.sdk.ui.widgets.TextWidget
import io.snabble.sdk.ui.widgets.customercard.CustomerCardWidget
import io.snabble.sdk.ui.widgets.purchase.ui.PurchaseWidget
import io.snabble.sdk.ui.widgets.stores.StartShoppingWidget

typealias OnDynamicAction = (action: DynamicAction) -> Unit

@Composable
fun DynamicView(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    background: @Composable (() -> Unit),
    widgets: List<Widget>,
    onAction: OnDynamicAction,
) {
    Box(
        modifier = modifier
    ) {
        background()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            items(items = widgets) { widget ->
                Widget(widget = widget, onAction)
            }
        }
    }
}

@Composable
fun Widget(
    widget: Widget,
    onAction: OnDynamicAction,
) = when (widget) {
    is ButtonItem -> {
        ButtonWidget(
            model = widget,
            onClick = onAction,
        )
    }
    is CustomerCardItem -> {
        CustomerCardWidget(
            model = widget,
            onAction = onAction,
        )
    }
    is ConnectWifiItem -> {
        ConnectWifiWidget(
            model = widget,
            onClick = onAction,
        )
    }
    is ImageItem -> {
        ImageWidget(
            model = widget,
            onClick = onAction,
        )
    }
    is InformationItem -> {
        InformationWidget(
            model = widget,
            onClick = onAction,
        )
    }
    is LocationPermissionItem -> {
        LocationPermissionWidget(
            model = widget,
            onAction = onAction,
        )
    }
    is PurchasesItem -> {
        PurchaseWidget(
            model = widget,
            onAction = onAction,
        )
    }
    is SeeAllStoresItem -> {
        SeeAllStoresWidget(
            model = widget,
            onClick = onAction,
        )
    }
    is StartShoppingItem -> {
        StartShoppingWidget(
            model = widget,
            onAction = onAction,
        )
    }
    is TextItem -> {
        TextWidget(
            model = widget,
            onClick = onAction,
        )
    }
    else -> Unit
}
