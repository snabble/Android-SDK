package io.snabble.sdk.ui.payment.telecash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.ui.payment.CreditCardInputView
import io.snabble.sdk.ui.payment.telecash.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.telecash.domain.CustomerInfo
import io.snabble.sdk.ui.payment.telecash.domain.TelecashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class TelecashViewModel(
    private val telecashRepo: TelecashRepository,
    private val countryItemsRepo: CountryItemsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        countryItemsRepo.loadCountryItems().xx("Countries")
    }

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val paymentMethod =
                savedStateHandle.get<PaymentMethod>(CreditCardInputView.ARG_PAYMENT_TYPE) ?: return@launch
            val result = telecashRepo.sendUserData(customerInfo, paymentMethod)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        formUrl = info.formUrl,
                        deletePreAuthUrl = it.deletePreAuthUrl
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, showError = true) }
            }
        }
    }

    fun errorHandled() {
        _uiState.update { it.copy(showError = false) }
    }
}

data class UiState(
    val isLoading: Boolean = false,
    val formUrl: String? = null,
    val deletePreAuthUrl: String? = null,
    val showError: Boolean = false
)
