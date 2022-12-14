package io.snabble.sdk.ui.payment.payone.sepa.form.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.IBAN
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import io.snabble.sdk.ui.payment.payone.sepa.form.domain.PayoneSepaFormRepository
import io.snabble.sdk.ui.payment.payone.sepa.form.domain.PayoneSepaFormRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PayoneSepaFormViewModel(
    val repository: PayoneSepaFormRepository = PayoneSepaFormRepositoryImpl(snabble = Snabble),
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
