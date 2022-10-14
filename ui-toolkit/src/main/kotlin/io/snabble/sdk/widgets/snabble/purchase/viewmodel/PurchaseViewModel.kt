package io.snabble.sdk.widgets.snabble.purchase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.widgets.snabble.purchase.Purchase
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

internal class PurchaseViewModel : ViewModel() {

    private val repo: PurchasesRepository by KoinProvider.inject()

    private val _state = MutableStateFlow<State>(Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    fun updatePurchases() {
        viewModelScope.launch {
            val purchases = loadPurchases()
            _state.value = ShowPurchases(purchases)
        }
    }

    private suspend fun loadPurchases(): List<Purchase> = repo.getPurchases(count = 2)
}

internal sealed class State
internal object Loading : State()
internal data class ShowPurchases(val purchases: List<Purchase>) : State()
