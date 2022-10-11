package io.snabble.sdk.dynamicview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.ConnectWifiItem
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.domain.model.Widget
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.widgets.ButtonWidget
import io.snabble.sdk.widgets.ImageWidget
import io.snabble.sdk.widgets.InformationWidget
import io.snabble.sdk.widgets.SectionWidget
import io.snabble.sdk.widgets.TextWidget
import io.snabble.sdk.widgets.snabble.customercard.CustomerCardWidget
import io.snabble.sdk.widgets.snabble.locationpermission.LocationPermissionWidget
import io.snabble.sdk.widgets.snabble.purchase.ui.PurchaseWidget
import io.snabble.sdk.widgets.snabble.stores.SeeAllStoresWidget
import io.snabble.sdk.widgets.snabble.stores.StartShoppingWidget
import io.snabble.sdk.widgets.snabble.toggle.ToggleWidget
import io.snabble.sdk.widgets.snabble.wifi.ConnectWifiWidget

typealias OnDynamicAction = (action: DynamicAction) -> Unit

@Composable
fun DynamicView(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    background: @Composable (() -> Unit),
    widgets: List<Widget>,
    onAction: OnDynamicAction,
) {
    Box(modifier = modifier) {
        background()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
    is ButtonItem -> ButtonWidget(model = widget, onAction = onAction)

    is CustomerCardItem -> CustomerCardWidget(model = widget, onAction = onAction)

    is ConnectWifiItem -> ConnectWifiWidget(model = widget, onAction = onAction)

    is ImageItem -> ImageWidget(model = widget, onAction = onAction)

    is InformationItem -> InformationWidget(model = widget, onAction = onAction)

    is LocationPermissionItem -> LocationPermissionWidget(model = widget, onAction = onAction)

    is PurchasesItem -> PurchaseWidget(model = widget, onAction = onAction)

    is SeeAllStoresItem -> SeeAllStoresWidget(model = widget, onAction = onAction)

    is StartShoppingItem -> StartShoppingWidget(model = widget, onAction = onAction)

    is TextItem -> TextWidget(
        modifier = Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null
        ) {},
        model = widget,
        onAction = onAction
    )

    is ToggleItem -> ToggleWidget(model = widget, onAction = onAction)

    is SectionItem -> SectionWidget(model = widget, onAction = onAction)

}
