package io.snabble.sdk.ui.widgets.stores.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StoresViewModel(
    private val snabble: Snabble,
) : ViewModel() {

    private val _isCheckedInFlow = MutableStateFlow(false)
    val isCheckedInFlow: StateFlow<Boolean> = _isCheckedInFlow.asStateFlow()

    private val observer = Observer<Shop?> { shop: Shop? ->
        _isCheckedInFlow.tryEmit(shop != null)
    }

    init {
        snabble.currentCheckedInShop.observeForever(observer)
    }

    override fun onCleared() {
        snabble.currentCheckedInShop.removeObserver(observer)

        super.onCleared()
    }
}
