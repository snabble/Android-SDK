package io.snabble.sdk.ui.widgets.purchase.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.ui.widgets.purchase.Purchase
import io.snabble.sdk.ui.widgets.purchase.repository.PurchasesRepository
import kotlinx.coroutines.launch

internal class PurchaseViewModel : ViewModel() {

    private val repo: PurchasesRepository by KoinProvider.getKoin().inject()

    var state: State by mutableStateOf(Loading)
        private set

    fun updatePurchases() {
        viewModelScope.launch {
            val purchases = loadPurchases()
            state = ShowPurchases(purchases)
        }
    }

    private suspend fun loadPurchases(): List<Purchase> = repo.getPurchases(count = 2)
}

internal sealed class State
internal object Loading : State()
internal data class ShowPurchases(val data: List<Purchase>) : State()
