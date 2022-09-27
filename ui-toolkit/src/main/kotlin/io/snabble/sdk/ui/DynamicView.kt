package io.snabble.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.home.HomeViewModel
import io.snabble.sdk.ui.widgets.ButtonWidget
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.ui.widgets.LocationPermissionWidget
import io.snabble.sdk.ui.widgets.SeeAllStoresWidget
import io.snabble.sdk.ui.widgets.StartShoppingWidget
import io.snabble.sdk.ui.widgets.TextWidget
import io.snabble.sdk.ui.widgets.purchase.ui.PurchaseScreen
import io.snabble.sdk.usecase.GetPermissionStateUseCase

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
            contentPadding = PaddingValues(16.dp),
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
fun Widget(
    widget: Widget,
    click: WidgetClick,
    viewModel: HomeViewModel = viewModel()
) = when (widget) {
    is ButtonItem -> {
        ButtonWidget(
            model = widget,
            onClick = click,
        )
    }
    is ImageItem -> {
        ImageWidget(
            model = widget,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    is LocationPermissionItem -> {

        val permissionIsGranted: Boolean by remember {
            val getPermissionStateUseCase = GetPermissionStateUseCase()
            getPermissionStateUseCase()
        }
        LocationPermissionWidget(
            model = widget,
            permissionState = permissionIsGranted,
            onClick = click
        )
    }
    is PurchasesItem -> {
        PurchaseScreen(model = widget)
    }
    is SeeAllStoresItem -> {
        SeeAllStoresWidget(
            model = widget,
            checkinState = viewModel.checkInState.value,
            onClick = click
        )
    }
    is StartShoppingItem -> {
        StartShoppingWidget(
            model = widget,
            checkinState = viewModel.checkInState.value,
            onClick = click
        )
    }
    is TextItem -> {
        TextWidget(
            model = widget,
            modifier = Modifier
                .clickable { click(widget.id) }
        )
    }
    else -> {}
}
