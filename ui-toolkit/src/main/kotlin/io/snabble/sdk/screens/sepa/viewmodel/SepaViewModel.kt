package io.snabble.sdk.screens.sepa.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.payment.IBAN
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData

class SepaViewModel : ViewModel() {

    fun validateIban(string: String): Boolean {
        return IBAN.validate(string)
    }

    fun validateText(string: String): Boolean {
        return string.isNotBlank()
    }

    fun saveData(data: PayoneSepaData) {
        //TODO: implement
    }
}
