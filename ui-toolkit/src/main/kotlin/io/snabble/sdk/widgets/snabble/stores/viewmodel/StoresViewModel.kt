package io.snabble.sdk.widgets.snabble.stores.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StoresViewModel(
    private val Snabble: Snabble,
) : ViewModel() {

    private val _isCheckedInFlow = MutableStateFlow(false)
    val isCheckedInFlow: StateFlow<Boolean> = _isCheckedInFlow.asStateFlow()

    private val isCheckedInObserver = Observer<Shop?> { shop: Shop? ->
        _isCheckedInFlow.tryEmit(shop != null)
    }

    init {
        Snabble.currentCheckedInShop.observeForever(isCheckedInObserver)
    }

    override fun onCleared() {
        Snabble.currentCheckedInShop.removeObserver(isCheckedInObserver)

        super.onCleared()
    }
}
