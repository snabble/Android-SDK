package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toolkit.R


@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun LocationPermissionPreview() {
    LocationPermissionWidget(
        model = LocationPermissionItem("1", 16),
        permissionState = false
    )
}

@Composable
fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    permissionState: Boolean = false,
    onClick: WidgetClick = {},
) {
    if (!permissionState) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.align(Alignment.Center),
                widget = model,
                text = stringResource(id = R.string.Snabble_askForPermission),
                onClick = onClick,
            )
        }
    }
}