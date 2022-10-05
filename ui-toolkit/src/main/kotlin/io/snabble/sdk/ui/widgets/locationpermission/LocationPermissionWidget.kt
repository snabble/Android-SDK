package io.snabble.sdk.ui.widgets.locationpermission

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
import io.snabble.sdk.ui.widgets.ButtonWidget
import org.koin.androidx.compose.getViewModel

@Composable
internal fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    viewModel: LocationPermissionViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    val launcher =
        createActivityResultLauncher(viewModel)

    if (!viewModel.permissionButtonIsVisible.collectAsState().value) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.fillMaxWidth(),
                widget = model,
                padding = model.padding,
                text = stringResource(id = R.string.Snabble_askForPermission),
                onAction = {
                    onAction(DynamicAction(widget = model))
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
            )
        }
    }
}

@Composable
private fun createActivityResultLauncher(viewModel: LocationPermissionViewModel) =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        viewModel.update(true)
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
