package io.snabble.sdk.ui.payment.fiserv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.fiserv.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.fiserv.domain.CustomerInfo
import io.snabble.sdk.ui.payment.fiserv.domain.FiservRepository
import io.snabble.sdk.ui.payment.fiserv.domain.model.country.CountryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FiservViewModel(
    private val fiservRepo: FiservRepository,
    countryItemsRepo: CountryItemsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(countryItems = countryItemsRepo.loadCountryItems()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val paymentMethod =
                savedStateHandle.get<PaymentMethod>(FiservInputView.ARG_PAYMENT_TYPE) ?: return@launch
            val result = fiservRepo.sendUserData(customerInfo, paymentMethod)
            result
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
