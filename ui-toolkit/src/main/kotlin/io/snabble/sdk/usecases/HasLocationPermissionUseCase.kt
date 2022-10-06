package io.snabble.sdk.usecases

import android.annotation.SuppressLint
import android.util.Log
import io.snabble.sdk.Snabble

internal class HasLocationPermissionUseCase(
    private val snabble: Snabble,
) {

    operator fun invoke(): Boolean =
        try {
            snabble.checkInLocationManager.checkLocationPermission()
        } catch (e: UninitializedPropertyAccessException) {
            Log.d(this.javaClass.name, "invokeError: ${e.message} ")
            true
        }
}

internal class UpdateCheckInManagerUseCase(
    private val hasLocationPermission: HasLocationPermissionUseCase,
    private val snabble: Snabble,
) {

    @SuppressLint("MissingPermission")
    operator fun invoke() {
        if (hasLocationPermission()) {
            snabble.checkInManager.startUpdating()
        } else {
            snabble.checkInManager.stopUpdating()
        }
    }
}
