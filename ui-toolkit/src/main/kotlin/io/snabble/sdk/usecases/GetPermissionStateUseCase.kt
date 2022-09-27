package io.snabble.sdk.usecases

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.snabble.sdk.Snabble

// FIXME: Scope should be internal
class GetPermissionStateUseCase {

    operator fun invoke(): MutableState<Boolean> = try {
        mutableStateOf(Snabble.checkInLocationManager.checkLocationPermission())
    } catch (e: UninitializedPropertyAccessException) {
        Log.d(this.javaClass.name, "invokeError: ${e.message} ")
        mutableStateOf(false)
    }
}
