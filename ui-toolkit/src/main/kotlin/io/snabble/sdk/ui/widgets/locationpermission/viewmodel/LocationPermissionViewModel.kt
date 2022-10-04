package io.snabble.sdk.ui.widgets.locationpermission

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetPermissionStateUseCase
import io.snabble.sdk.usecases.UpdateChechkinManagerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LocationPermissionViewModel(
    getPermissionState: GetPermissionStateUseCase,
    private val updateChechkinManager: UpdateChechkinManagerUseCase,
) : ViewModel() {

    private val _permissionButtonIsVisible = MutableStateFlow(getPermissionState())
    val permissionButtonIsVisible: StateFlow<Boolean> = _permissionButtonIsVisible

    internal fun update(showPermissionButton: Boolean) {
        updatePermissionButtonIsVisible(showPermissionButton)
        updateChechkinManager()
    }

    private fun updatePermissionButtonIsVisible(showPermissionButton: Boolean) {
        _permissionButtonIsVisible.tryEmit(showPermissionButton)
    }
}
