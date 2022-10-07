package io.snabble.sdk.usecases

import android.annotation.SuppressLint
import io.snabble.sdk.Snabble

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
