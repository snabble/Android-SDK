package io.snabble.sdk.widgets.snabble.locationpermission.di

import io.snabble.sdk.widgets.snabble.locationpermission.usecases.HasLocationPermissionUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.usecases.HasLocationPermissionUseCaseImpl
import io.snabble.sdk.widgets.snabble.locationpermission.usecases.UpdateCheckInManagerUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.usecases.UpdateCheckInManagerUseCaseImpl
import io.snabble.sdk.widgets.snabble.locationpermission.viewmodel.LocationPermissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val locationPermissionModule = module {
    viewModelOf(::LocationPermissionViewModel)
    factoryOf(::HasLocationPermissionUseCaseImpl) bind HasLocationPermissionUseCase::class
    factoryOf(::UpdateCheckInManagerUseCaseImpl) bind UpdateCheckInManagerUseCase::class
}
