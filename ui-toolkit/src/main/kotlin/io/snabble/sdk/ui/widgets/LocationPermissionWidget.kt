package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toolkit.R

@Composable
fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    permissionState: Boolean = false,
    onClick: OnDynamicAction,
) {
    if (!permissionState) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.fillMaxWidth(),
                widget = model,
                text = stringResource(id = R.string.Snabble_askForPermission),
                onClick = onClick,
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun LocationPermissionPreview() {
    LocationPermissionWidget(
        model = LocationPermissionItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        permissionState = false,
        onClick = {},
    )
}
