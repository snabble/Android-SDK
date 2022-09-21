package io.snabble.sdk.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.snabble.sdk.Snabble

class HomeViewModel : ViewModel() {

    companion object {
        val instance = HomeViewModel()
    }

    val checkInState: MutableState<Boolean>
        get() {
            var state = mutableStateOf(Snabble.currentCheckedInShop.value != null)
            Snabble.currentCheckedInShop.observeForever {
                state = mutableStateOf(it != null)
            }
            return state
        }

    var widgetEvent = MutableLiveData<String>()

    fun onClick(string: String) {
        widgetEvent.postValue(string)
    }

}
