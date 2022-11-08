package io.snabble.sdk.widgets.snabble.purchase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.widgets.snabble.purchase.Purchase
import io.snabble.sdk.widgets.snabble.purchase.usecases.GetPurchasesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PurchaseViewModel(
    private val getPurchases: GetPurchasesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(Loading)
    val state = _state.asStateFlow()

    fun updatePurchases() {
        viewModelScope.launch {
            val purchases = loadPurchases()
            _state.value = ShowPurchases(purchases)
        }
    }

    private suspend fun loadPurchases(): List<Purchase> = getPurchases(count = 2)
}

internal sealed class State
internal object Loading : State()
internal data class ShowPurchases(val purchases: List<Purchase>) : State()
