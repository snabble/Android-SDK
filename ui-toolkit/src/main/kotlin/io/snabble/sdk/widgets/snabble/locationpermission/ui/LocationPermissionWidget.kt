package io.snabble.sdk.widgets.snabble.locationpermission.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.LocationPermission
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
    if (!hasLocationPermissionState.value) {
        LocationPermission(
            modifier = modifier,
            model = model,
            onAction = {
                onAction(it)
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        )
    }
}

@Composable
private fun createActivityResultLauncher(viewModel: LocationPermissionViewModel) =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.update(hasPermission = true)
    }
