package io.snabble.sdk.widgets.snabble.locationpermission.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.widgets.snabble.locationpermission.usecases.HasLocationPermissionUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.usecases.UpdateCheckInManagerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class LocationPermissionViewModel(
    hasLocationPermission: HasLocationPermissionUseCase,
    private val updateCheckInManager: UpdateCheckInManagerUseCase,
) : ViewModel() {

    private val _hasLocationPermission = MutableStateFlow(hasLocationPermission())
    val hasLocationPermission = _hasLocationPermission.asStateFlow()

    internal fun update(hasPermission: Boolean) {
        updateLocationPermission(hasPermission)
        updateCheckInManager()
    }

    private fun updateLocationPermission(hasPermission: Boolean) {
        _hasLocationPermission.tryEmit(hasPermission)
    }
}
