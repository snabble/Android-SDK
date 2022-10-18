package io.snabble.sdk.widgets.snabble.locationpermission.usecases

import android.annotation.SuppressLint
import io.snabble.sdk.Snabble

internal interface UpdateCheckInManagerUseCase {

    operator fun invoke()
}

internal class UpdateCheckInManagerUseCaseImpl(
    private val hasLocationPermission: HasLocationPermissionUseCase,
    private val Snabble: Snabble,
) : UpdateCheckInManagerUseCase {

    @SuppressLint("MissingPermission")
    override operator fun invoke() {
        if (hasLocationPermission()) {
            Snabble.checkInManager.startUpdating()
        } else {
            Snabble.checkInManager.stopUpdating()
        }
    }
}
