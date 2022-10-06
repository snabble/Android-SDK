package io.snabble.sdk.usecases

import android.annotation.SuppressLint
import android.util.Log
import io.snabble.sdk.Snabble

internal class GetPermissionStateUseCase(
    private val snabble: Snabble
) {

    operator fun invoke(): Boolean =
        try {
            snabble.checkInLocationManager.checkLocationPermission()
        } catch (e: UninitializedPropertyAccessException) {
            Log.d(this.javaClass.name, "invokeError: ${e.message} ")
            true
        }
}

internal class UpdateChechkinManagerUseCase(
    private val getPermissionState: GetPermissionStateUseCase,
    private val snabble: Snabble,
) {

    @SuppressLint("MissingPermission")
    operator fun invoke() {
        if (getPermissionState()) {
            snabble.checkInManager.startUpdating()
        } else {
            snabble.checkInManager.stopUpdating()
        }
    }
}