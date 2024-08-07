package io.snabble.sdk.ui.payment.datatrans.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.datatrans.domain.DatatransRepository
import io.snabble.sdk.ui.payment.datatrans.domain.model.CustomerInfo
import io.snabble.sdk.ui.payment.fiserv.FiservInputView
import io.snabble.sdk.ui.payment.fiserv.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.fiserv.domain.model.country.CountryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DatatransViewModel(
    private val datatransRepository: DatatransRepository,
    countryItemsRepo: CountryItemsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(UiState(countryItems = countryItemsRepo.loadCountryItems())) //TBI: change to datatrans country version
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val paymentMethod = savedStateHandle.get<PaymentMethod>(FiservInputView.ARG_PAYMENT_TYPE)

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            paymentMethod ?: return@launch
            datatransRepository.sendUserData(customerInfo, paymentMethod)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mobileToken = info.mobileToken,
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
    val countryItems: List<CountryItem>,
    val mobileToken: String? = null,
    val showError: Boolean = false
)
