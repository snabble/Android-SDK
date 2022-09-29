package io.snabble.sdk.usecases.di

import io.snabble.sdk.usecases.GetAvailableWifiUseCase
import io.snabble.sdk.usecases.GetCustomerCardInfo
import io.snabble.sdk.usecases.GetHomeConfigUseCase
import io.snabble.sdk.usecases.GetPermissionStateUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val useCaseModule = module {
    factoryOf(::GetAvailableWifiUseCase)
    factoryOf(::GetCustomerCardInfo)
    factoryOf(::GetHomeConfigUseCase)
    factoryOf(::GetPermissionStateUseCase)
}
