package io.snabble.sdk.widgets.snabble.locationpermission.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.widgets.snabble.locationpermission.domain.HasLocationPermissionUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.domain.UpdateCheckInManagerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LocationPermissionViewModel(
    hasLocationPermission: HasLocationPermissionUseCase,
    private val updateCheckInManager: UpdateCheckInManagerUseCase,
) : ViewModel() {

    private val _hasLocationPermission = MutableStateFlow(hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission

    internal fun update(hasPermission: Boolean) {
        updateLocationPermission(hasPermission)
        updateCheckInManager()
    }

    private fun updateLocationPermission(hasPermission: Boolean) {
        _hasLocationPermission.tryEmit(hasPermission)
    }
}