package io.snabble.sdk.ui.payment.creditcard.fiserv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.FiservRepositoryImpl
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.FiservCountryDto
import io.snabble.sdk.ui.payment.creditcard.shared.data.CountryItemsRepositoryImpl
import io.snabble.sdk.ui.payment.creditcard.shared.data.country.LocalCountryItemsDataSourceImpl
import io.snabble.sdk.ui.payment.creditcard.shared.data.displayName
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.StateItem
import io.snabble.sdk.utils.GsonHolder

class FiservViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

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
                    gson = GsonHolder.get(),
                    clazz = FiservCountryDto::class.java,
                    mapFrom = { dto ->
                        CountryItem(
                            displayName = dto.countryCode.displayName,
                            code = dto.countryCode,
                            stateItems = dto.states?.map { StateItem.from(it) }
                        )
                    }
                )
            ),
            savedStateHandle = savedStateHandle
        ) as T
    }
}
