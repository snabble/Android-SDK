package io.snabble.sdk.ui.payment.externalbilling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.externalbilling.ExternalBillingRepositoryImpl
import io.snabble.sdk.payment.externalbilling.data.ExternalBillingPaymentCredentials
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.utils.GsonHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalBillingViewModel : ViewModel() {

    private var _uiState = MutableStateFlow<UiState>(Processing)
    internal val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun login(idDescriptor: String, username: String, password: String, projectId: String): Boolean {
        viewModelScope.launch {
            val project = Snabble.projects.find { it.id == projectId }
            project?.let {
                val repo = ExternalBillingRepositoryImpl(
                    it,
                    GsonHolder.get()
                )
                val result = repo.login(username, password)
                if (result.isSuccess) {
                    val billingCredentials = result.getOrNull()
                    billingCredentials?.let { bc ->
                        val paymentCredentials: PaymentCredentials? = PaymentCredentials.fromExternalBilling(
                            ExternalBillingPaymentCredentials(
                                username = username,
                                contactPersonID = bc.contactPersonID,
                                password = password
                            ),
                            projectId,
                            "$idDescriptor: $username",
                        )
                        val success = repo.addPaymentCredentials(paymentCredentials)
                        val type = paymentCredentials?.type
                        if (success && type != null) {
                            trackCredentialType(type.name)
                        }
                    }
                    _uiState.tryEmit(Success)
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.tryEmit(Error(error?.localizedMessage.toString()))
                }
            }
        }
        return false
    }

    fun typing() {
        _uiState.tryEmit(Processing)
    }

    private fun trackCredentialType(type: String) {
        Telemetry.event(Telemetry.Event.PaymentMethodAdded, type)
    }
}

sealed interface UiState

object Processing : UiState

object Success : UiState

data class Error(
    val message: String
) : UiState
