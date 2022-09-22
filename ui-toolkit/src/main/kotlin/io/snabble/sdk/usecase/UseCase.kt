package io.snabble.sdk.usecase

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.snabble.sdk.Snabble
import io.snabble.sdk.config.ConfigFileProviderImpl
import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.Root
import kotlinx.serialization.json.Json

class GetPermissionStateUseCase {

    operator fun invoke(): MutableState<Boolean> {
        return try {
            mutableStateOf(Snabble.checkInLocationManager.checkLocationPermission())
        } catch (e: UninitializedPropertyAccessException) {
            Log.d(this.javaClass.name, "invokeError: ${e.message} ")
            mutableStateOf(false)
        }
    }
}

class GetHomeConfigUseCase {

    suspend operator fun invoke(context: Context): Root {

        val repo = ConfigRepository(
            ConfigFileProviderImpl(context.resources.assets),
            Json
        )

        val rootDto = repo.getConfig<RootDto>("homeConfig.json")
        return ConfigMapperImpl(context).mapTo(rootDto)


    }
}

class GetCustomerCardInfo() {

    operator fun invoke(): MutableState<Boolean> {
        // TODO: Evaluate wether to take checkedInProject or like given
        return mutableStateOf(Snabble.projects.first().customerCardInfo.isNotEmpty())
    }
}