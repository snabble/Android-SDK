package io.snabble.sdk.ui.widgets.stores

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.snabble.sdk.Snabble

internal class StoresViewModel(
    private val snabble: Snabble
) : ViewModel() {

    val checkInState: MutableState<Boolean>
        get() {
            val state = mutableStateOf(Snabble.currentCheckedInShop.value != null)
            snabble.currentCheckedInShop.observeForever { shop ->
                state.value = shop != null
            }
            return state
        }

}
