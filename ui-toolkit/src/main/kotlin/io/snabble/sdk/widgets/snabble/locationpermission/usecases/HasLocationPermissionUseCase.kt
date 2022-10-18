package io.snabble.sdk.widgets.snabble.locationpermission.usecases

import android.util.Log
import io.snabble.sdk.Snabble

internal interface HasLocationPermissionUseCase {

    operator fun invoke(): Boolean
}

internal class HasLocationPermissionUseCaseImpl(
    private val Snabble: Snabble,
) : HasLocationPermissionUseCase {

    override operator fun invoke(): Boolean =
        try {
            Snabble.checkInLocationManager.checkLocationPermission()
        } catch (e: UninitializedPropertyAccessException) {
            Log.d(this.javaClass.name, "invokeError: ${e.message} ")
            true
        }
}
