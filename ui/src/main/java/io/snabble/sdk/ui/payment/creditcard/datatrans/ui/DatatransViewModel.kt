package io.snabble.sdk.ui.payment.creditcard.datatrans.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.datatrans.payment.api.Transaction
import ch.datatrans.payment.api.TransactionListener
import ch.datatrans.payment.api.TransactionSuccess
import ch.datatrans.payment.exception.TransactionException
import ch.datatrans.payment.paymentmethods.CardExpiryDate
import ch.datatrans.payment.paymentmethods.SavedCard
import ch.datatrans.payment.paymentmethods.SavedPaymentMethod
import ch.datatrans.payment.paymentmethods.SavedPostFinanceCard
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.DatatransRepository
import io.snabble.sdk.ui.payment.creditcard.datatrans.ui.DatatransFragment.Companion.ARG_PAYMENT_TYPE
import io.snabble.sdk.ui.payment.creditcard.datatrans.ui.DatatransFragment.Companion.ARG_PROJECT_ID
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo
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

    private val _uiState = MutableStateFlow(UiState(countryItems = countryItemsRepo.loadCountryItems()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _event: MutableStateFlow<Event?> = MutableStateFlow(null)
    val event = _event.asStateFlow()

    private val paymentMethod = savedStateHandle.get<PaymentMethod>(ARG_PAYMENT_TYPE)
    private val projectId = savedStateHandle.get<String>(ARG_PROJECT_ID)

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            paymentMethod ?: return@launch
            datatransRepository.sendUserData(customerInfo, paymentMethod)
                .onSuccess { info ->
                    _uiState.update { it.copy(isLoading = false) }
                    _event.update { Event.TransActionCreated(createTransaction(info.mobileToken, info.isTesting)) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, showError = true) }
                }
        }
    }

    private fun createTransaction(token: String, isTesting: Boolean) = Transaction(token).apply {
        listener = object : TransactionListener {
            override fun onTransactionSuccess(result: TransactionSuccess) {
                val datatransToken = when (val savedPaymentMethod = result.savedPaymentMethod) {
                    is SavedPostFinanceCard -> DatatransToken(savedPaymentMethod, savedPaymentMethod.cardExpiryDate)

                    is SavedCard -> DatatransToken(
                        savedPaymentMethod,
                        savedPaymentMethod.cardExpiryDate
                    )

                    else -> {
                        if (savedPaymentMethod != null) DatatransToken(savedPaymentMethod)
                        null
                    }
                }
                when {
                    datatransToken != null -> _event.update { Event.TransActionSucceeded(datatransToken) }
                    else -> _event.update { Event.TransactionFailed }
                }
            }

            override fun onTransactionError(exception: TransactionException) {
                _event.update { Event.TransactionFailed }
            }
        }
        options.appCallbackScheme = "snabble"
        options.isTesting = isTesting
        options.useCertificatePinning = true
    }

    fun errorHandled() {
        _uiState.update { it.copy(showError = false) }
    }

    fun saveDatatransToken(datatransToken: DatatransToken, displayName: String) {
        val credentials = PaymentCredentials.fromDatatrans(
            datatransToken.token.alias,
            PaymentCredentials.Brand.fromPaymentMethod(paymentMethod),
            displayName,
            datatransToken.expiryDate?.formattedMonth.orEmpty(),
            datatransToken.expiryDate?.formattedYear.orEmpty(),
            projectId,
        )
        Snabble.paymentCredentialsStore.add(credentials)
        _event.update { Event.Finish }
    }
}

internal data class UiState(
    val isLoading: Boolean = false,
    val countryItems: List<CountryItem>,
    val mobileToken: String? = null,
    val showError: Boolean = false,
    val isTesting: Boolean = false,
    val transaction: Transaction? = null,
    val datatransToken: DatatransToken? = null
)

internal sealed interface Event {
    data object TransactionFailed : Event
    data class TransActionCreated(val transaction: Transaction) : Event
    data class TransActionSucceeded(val datatransToken: DatatransToken) : Event
    data object Finish: Event
}

internal data class DatatransToken(
    val token: SavedPaymentMethod,
    val expiryDate: CardExpiryDate? = null
)
