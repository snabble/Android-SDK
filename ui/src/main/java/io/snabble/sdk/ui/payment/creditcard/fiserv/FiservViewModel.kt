package io.snabble.sdk.ui.payment.creditcard.fiserv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.fiserv.domain.FiservRepository
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FiservViewModel(
    private val fiservRepo: FiservRepository,
    countryItemsRepo: CountryItemsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(countryItems = countryItemsRepo.loadCountryItems()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val paymentMethod = savedStateHandle.get<PaymentMethod>(FiservInputView.ARG_PAYMENT_TYPE)

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            paymentMethod ?: return@launch

            _uiState.update { it.copy(isLoading = true, showError = false) }

            fiservRepo.sendUserData(customerInfo, paymentMethod)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            formUrl = info.formUrl,
                            deletePreAuthUrl = info.preAuthDeleteUrl
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, showError = true) }
                }
        }
    }

    fun errorHandled() {
        _uiState.update { it.copy(showError = false) }
    }
}

internal data class UiState(
    val isLoading: Boolean = false,
    val formUrl: String? = null,
    val countryItems: List<CountryItem>,
    val deletePreAuthUrl: String? = null,
    val showError: Boolean = false
)
