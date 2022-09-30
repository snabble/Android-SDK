package io.snabble.sdk.ui.widgets.locationpermission

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetPermissionStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LocationPermissionViewModel(
    getPermissionState: GetPermissionStateUseCase
) : ViewModel() {

    private val _permissionState = MutableStateFlow(getPermissionState())
    val permissionState: StateFlow<Boolean> = _permissionState

    internal fun updatePermissionState(hasPermission: Boolean) {
        _permissionState.tryEmit(hasPermission)
    }

}

