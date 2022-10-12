package io.snabble.sdk.widgets.snabble.locationpermission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.ButtonWidget
import io.snabble.sdk.widgets.snabble.locationpermission.viewmodel.LocationPermissionViewModel
import org.koin.androidx.compose.getViewModel

@Composable
internal fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    viewModel: LocationPermissionViewModel = getViewModel(scope = KoinProvider.scope),
    onAction: OnDynamicAction,
) {
    val launcher = createActivityResultLauncher(viewModel)

    @OptIn(ExperimentalLifecycleComposeApi::class)
    val hasLocationPermissionState = viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    LocationPermission(
        modifier = modifier,
        model = model,
        isButtonVisible = !hasLocationPermissionState.value,
        onAction = {
            onAction(it)
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    )
}

@Composable
fun LocationPermission(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    isButtonVisible: Boolean,
    onAction: OnDynamicAction,
) {
    if (isButtonVisible) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ButtonWidget(
                modifier = modifier.fillMaxWidth(),
                widget = model,
                padding = model.padding,
                text = stringResource(id = R.string.Snabble_askForPermission),
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun createActivityResultLauncher(viewModel: LocationPermissionViewModel) =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.update(hasPermission = true)
    }

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun LocationPermissionPreview() {
    LocationPermission(
        model = LocationPermissionItem(
            id = "1",
            padding = Padding(horizontal = 16, vertical = 5)
        ),
        isButtonVisible = true,
        onAction = {},
    )
}
