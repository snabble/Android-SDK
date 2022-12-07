package io.snabble.sdk.ui.payment.payone.sepa.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.IBAN
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import io.snabble.sdk.ui.payment.payone.sepa.domain.PayoneSepaRepository
import io.snabble.sdk.ui.payment.payone.sepa.domain.PayoneSepaRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SepaViewModel(
    val repository: PayoneSepaRepository = PayoneSepaRepositoryImpl(snabble = Snabble),
) : ViewModel() {

    private var _isIbanValid = MutableStateFlow(false)
    internal val isIbanValid = _isIbanValid.asStateFlow()

    fun validateIban(string: String) {
        _isIbanValid.tryEmit(IBAN.validate(string))
    }

    /**
     * True if the PayoneSepaData has been saved, false otherwise.
     */
    fun saveData(data: PayoneSepaData): Boolean = repository.saveSepaData(data)
}
