package io.snabble.sdk.ui.payment.telecash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.data.TelecashRemoteDataSourceImpl
import io.snabble.sdk.ui.payment.telecash.data.TelecashRepositoryImpl
import io.snabble.sdk.ui.payment.telecash.domain.UserDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TelecashViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {

    val _uiState = MutableStateFlow("")
    val uiState = _uiState.asStateFlow()

    fun preuAuth(userDetails: UserDetails) {
        viewModelScope.launch {
            val paymentMethod = handle.get<PaymentMethod>("paymentType") ?: return@launch
            val result = TelecashRepositoryImpl(TelecashRemoteDataSourceImpl()).preAuth(userDetails, paymentMethod)
            result.onSuccess { pre ->
                _uiState.update { pre.formUrl }
            }
        }
    }
}
