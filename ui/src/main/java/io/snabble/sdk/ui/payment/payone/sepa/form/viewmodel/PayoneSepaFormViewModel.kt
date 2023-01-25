package io.snabble.sdk.ui.payment.payone.sepa.form.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.IBAN
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import io.snabble.sdk.ui.payment.payone.sepa.form.domain.PayoneSepaFormRepository
import io.snabble.sdk.ui.payment.payone.sepa.form.domain.PayoneSepaFormRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PayoneSepaFormViewModel(
    private val repository: PayoneSepaFormRepository = PayoneSepaFormRepositoryImpl(snabble = Snabble),
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var _isIbanValid = MutableStateFlow(false)
    internal val isIbanValid: StateFlow<Boolean> = _isIbanValid.asStateFlow()

    private var _areAllInputsValid = MutableStateFlow(false)
    internal val areAllInputsValid: StateFlow<Boolean> = _areAllInputsValid.asStateFlow()

    private val ibanDigits: String? = savedStateHandle.get<String?>(ARG_IBAN)?.substring(startIndex = 2)
    private var _ibanNumber = MutableStateFlow(ibanDigits)
    internal val ibanNumber: StateFlow<String?> = _ibanNumber.asStateFlow()

    private var _name = MutableStateFlow("")
    internal val name: StateFlow<String> = _name.asStateFlow()

    private var _city = MutableStateFlow("")
    internal val city: StateFlow<String> = _city.asStateFlow()

    init {
        ibanNumber
            .onEach { iban -> _isIbanValid.tryEmit(IBAN.validate("$COUNTRY_CODE$iban")) }
            .launchIn(viewModelScope)
    }

    fun onIbanNumberChange(iban: String) {
        _ibanNumber.tryEmit(iban)
        updateInputsValid()
    }

    fun onNameChange(name: String) {
        _name.tryEmit(name)
        updateInputsValid()
    }

    fun onCityChange(city: String) {
        _city.tryEmit(city)
        updateInputsValid()
    }

    private fun updateInputsValid() {
        _areAllInputsValid.tryEmit(areAllInputsValid())
    }

    private fun areAllInputsValid() = name.value.isNotBlank() && city.value.isNotBlank() && isIbanValid.value

    /**
     * True if the PayoneSepaData is valid and has been saved, false otherwise.
     */
    fun saveData(): Boolean {
        if (!areAllInputsValid()) return false

        val data = PayoneSepaData(
            name = name.value,
            iban = "$COUNTRY_CODE${ibanNumber.value}",
            city = city.value,
            countryCode = COUNTRY_CODE
        )
        return repository.saveSepaData(data)
    }

    companion object {

        private const val COUNTRY_CODE = "DE"

        const val ARG_IBAN = "iban"

        val Factory: ViewModelProvider.Factory = viewModelFactory {

            initializer {
                val savedStateHandle = createSavedStateHandle()
                PayoneSepaFormViewModel(savedStateHandle = savedStateHandle)
            }
        }
    }
}
