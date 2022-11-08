package io.snabble.sdk.widgets.snabble.locationpermission.usecases

import android.annotation.SuppressLint
import io.snabble.sdk.Snabble

internal interface UpdateCheckInManagerUseCase {

    operator fun invoke()
}

internal class UpdateCheckInManagerUseCaseImpl(
    private val hasLocationPermission: HasLocationPermissionUseCase,
    private val snabble: Snabble,
) : UpdateCheckInManagerUseCase {

    @SuppressLint("MissingPermission")
    override operator fun invoke() {
        if (hasLocationPermission()) {
            snabble.checkInManager.startUpdating()
        } else {
            snabble.checkInManager.stopUpdating()
        }
    }
}
