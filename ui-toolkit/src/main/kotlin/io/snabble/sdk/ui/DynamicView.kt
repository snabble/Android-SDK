package io.snabble.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.home.HomeViewModel
import io.snabble.sdk.ui.widgets.ButtonWidget
import io.snabble.sdk.ui.widgets.ConnectWifiWidget
import io.snabble.sdk.ui.widgets.CustomerCardWidget
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.ui.widgets.InformationWidget
import io.snabble.sdk.ui.widgets.LocationPermissionWidget
import io.snabble.sdk.ui.widgets.SeeAllStoresWidget
import io.snabble.sdk.ui.widgets.StartShoppingWidget
import io.snabble.sdk.ui.widgets.TextWidget
import io.snabble.sdk.usecase.GetAvailableWifiUseCase

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
fun Widget(
    widget: Widget,
    click: WidgetClick,
    viewModel: HomeViewModel = viewModel()
) = when (widget) {
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
    is LocationPermissionItem -> {
        LocationPermissionWidget(
            model = widget,
            permissionState = viewModel.permissionState.value,
            onClick = click
        )
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
    is CustomerCardItem -> {
        CustomerCardWidget(
            model = widget,
            isVisible = viewModel.customerCardVisibilityState.value,
            onClick = { click(widget.id) })
    }
    is InformationItem -> {
        InformationWidget(
            model = widget,
            onclick = { click(widget.id) })
    }
    is ConnectWifiItem -> {
        ConnectWifiWidget(
            model = widget,
            onclick = { click(widget.id) },
            isVisible = GetAvailableWifiUseCase(LocalContext.current)().value
        )
    }
    else -> {}
}
