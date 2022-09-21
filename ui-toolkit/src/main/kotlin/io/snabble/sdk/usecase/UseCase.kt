package io.snabble.sdk.usecase

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.snabble.sdk.Snabble

interface GetUseCase {
    operator fun invoke(): MutableState<Boolean>
}

class GetPermissionStateUseCase : GetUseCase {

    override fun invoke(): MutableState<Boolean> {
        return try {
            mutableStateOf(Snabble.checkInLocationManager.checkLocationPermission())
        } catch (e: UninitializedPropertyAccessException) {
            Log.d(this.javaClass.name, "invokeError: ${e.message} ")
            mutableStateOf(false)
        }
    }
}