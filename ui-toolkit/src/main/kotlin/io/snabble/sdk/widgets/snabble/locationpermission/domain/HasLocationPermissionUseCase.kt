package io.snabble.sdk.widgets.snabble.locationpermission.domain

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
