package io.snabble.sdk.ui.payment.creditcard.fiserv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.FiservRepositoryImpl
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.CountryItemsRepositoryImpl
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.source.LocalCountryItemsDataSourceImpl
import io.snabble.sdk.utils.GsonHolder

internal class FiservViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (!modelClass.isAssignableFrom(FiservViewModel::class.java)) {
            throw IllegalArgumentException("Unable to construct viewmodel")
        }

        val savedStateHandle = extras.createSavedStateHandle()
        @Suppress("UNCHECKED_CAST")
        return FiservViewModel(
            fiservRepo = FiservRepositoryImpl(),
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
