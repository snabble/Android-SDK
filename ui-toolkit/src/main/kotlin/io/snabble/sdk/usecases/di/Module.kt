package io.snabble.sdk.usecases.di

import io.snabble.sdk.usecases.GetAvailableWifiUseCase
import io.snabble.sdk.usecases.HasCustomerCardUseCase
import io.snabble.sdk.usecases.GetHomeConfigUseCase
import io.snabble.sdk.usecases.HasLocationPermissionUseCase
import io.snabble.sdk.usecases.GetProfileConfigUseCase
import io.snabble.sdk.usecases.UpdateCheckInManagerUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val useCaseModule = module {
    factoryOf(::GetAvailableWifiUseCase)
    factoryOf(::HasCustomerCardUseCase)
    factoryOf(::GetHomeConfigUseCase)
    factoryOf(::GetProfileConfigUseCase)
    factoryOf(::HasLocationPermissionUseCase)
    factoryOf(::UpdateCheckInManagerUseCase)
}
