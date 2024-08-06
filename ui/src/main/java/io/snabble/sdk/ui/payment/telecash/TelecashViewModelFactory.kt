package io.snabble.sdk.ui.payment.telecash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.snabble.sdk.ui.payment.telecash.data.CountryItemsRepositoryImpl
import io.snabble.sdk.ui.payment.telecash.data.TelecashRepositoryImpl
import io.snabble.sdk.ui.payment.telecash.data.country.LocalCountryItemsDataSourceImpl
import io.snabble.sdk.utils.GsonHolder

class TelecashViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (!modelClass.isAssignableFrom(TelecashViewModel::class.java)) {
            throw IllegalArgumentException("Unable to construct viewmodel")
        }

        val savedStateHandle = extras.createSavedStateHandle()
        @Suppress("UNCHECKED_CAST")
        return TelecashViewModel(
            telecashRepo = TelecashRepositoryImpl(),
            countryItemsRepo = CountryItemsRepositoryImpl(
                localCountryItemsDataSource = LocalCountryItemsDataSourceImpl(
                    assetManager = context.assets,
                    gson = GsonHolder.get()
                )
            ),
            savedStateHandle = savedStateHandle
        ) as T
    }
}
