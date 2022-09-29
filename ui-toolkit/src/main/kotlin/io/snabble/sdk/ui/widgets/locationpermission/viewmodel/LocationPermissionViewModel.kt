package io.snabble.sdk.ui.widgets.locationpermission

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetPermissionStateUseCase

internal class LocationPermissionViewModel(
    getPermissionState: GetPermissionStateUseCase
) : ViewModel() {

    var permissionState = getPermissionState()
}

