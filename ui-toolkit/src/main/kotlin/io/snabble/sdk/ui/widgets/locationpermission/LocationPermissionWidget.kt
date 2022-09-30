package io.snabble.sdk.ui.widgets

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.locationpermission.LocationPermissionViewModel
import org.koin.androidx.compose.getViewModel

@Composable
internal fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    viewModel: LocationPermissionViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
        viewModel.updatePermissionState(true)
    }

    if (!viewModel.permissionState.collectAsState().value) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.fillMaxWidth(),
                widget = model,
                text = stringResource(id = R.string.Snabble_askForPermission),
                onClick = {
                    onAction(DynamicAction(widget = model))
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
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
        onAction = {},
    )
}
