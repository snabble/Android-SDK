package io.snabble.sdk.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.snabble.sdk.usecases.GetHomeConfigUseCase
import io.snabble.sdk.usecases.GetPermissionStateUseCase

// FIXME: Delete me!
class HomeViewModelFactory(
    private val getPermissionState: GetPermissionStateUseCase,
    private val getHomeConfig: GetHomeConfigUseCase,
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(getPermissionState, getHomeConfig) as T
    }
}
