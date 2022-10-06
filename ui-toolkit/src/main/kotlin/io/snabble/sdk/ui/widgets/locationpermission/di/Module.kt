package io.snabble.sdk.ui.widgets.locationpermission.di

import io.snabble.sdk.ui.widgets.locationpermission.viewmodel.LocationPermissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val locationPermissionModule = module {
    viewModelOf(::LocationPermissionViewModel)
}
