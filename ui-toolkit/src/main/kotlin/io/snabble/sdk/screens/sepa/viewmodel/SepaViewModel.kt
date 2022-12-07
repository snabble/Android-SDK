package io.snabble.sdk.screens.sepa.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.payment.IBAN
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SepaViewModel : ViewModel() {

    private var _showError = MutableStateFlow(false)
    internal val showError = _showError.asStateFlow()

    fun validateIban(string: String) {
        _showError.tryEmit(IBAN.validate(string))
    }

    fun saveData(data: PayoneSepaData) {
        //TODO: implement
    }
}
