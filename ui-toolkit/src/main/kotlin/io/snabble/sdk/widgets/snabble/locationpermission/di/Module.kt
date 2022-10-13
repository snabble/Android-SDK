package io.snabble.sdk.widgets.snabble.locationpermission.di

import io.snabble.sdk.widgets.snabble.locationpermission.domain.HasLocationPermissionUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.domain.UpdateCheckInManagerUseCase
import io.snabble.sdk.widgets.snabble.locationpermission.viewmodel.LocationPermissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val locationPermissionModule = module {
    viewModelOf(::LocationPermissionViewModel)
    factoryOf(::HasLocationPermissionUseCase)
    factoryOf(::UpdateCheckInManagerUseCase)
}
