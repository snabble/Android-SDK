package io.snabble.sdk.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.ui.toolkit.R

@Composable
fun LocationPermission(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    onAction: OnDynamicAction,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ButtonWidget(
            modifier = modifier.fillMaxWidth(),
            widget = model,
            padding = model.padding,
            text = stringResource(id = R.string.Snabble_DynamicView_LocationPermission_Button_notDetermined),
            onAction = onAction,
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun LocationPermissionPreview() {
    LocationPermission(
        model = LocationPermissionItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        onAction = {},
    )
}
