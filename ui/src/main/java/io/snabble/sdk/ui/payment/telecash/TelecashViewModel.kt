package io.snabble.sdk.ui.payment.telecash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.ui.payment.telecash.data.TelecashRemoteDataSourceImpl
import io.snabble.sdk.ui.payment.telecash.data.TelecashRepositoryImpl
import io.snabble.sdk.ui.payment.telecash.domain.UserDetails
import kotlinx.coroutines.launch

class TelecashViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {

    fun preuAuth(userDetails: UserDetails) {
        viewModelScope.launch {
            val paymentMethod = handle.get<PaymentMethod>("paymentType") ?: return@launch
            TelecashRepositoryImpl(TelecashRemoteDataSourceImpl()).preAuth(userDetails, paymentMethod).xx()
        }
    }
}
