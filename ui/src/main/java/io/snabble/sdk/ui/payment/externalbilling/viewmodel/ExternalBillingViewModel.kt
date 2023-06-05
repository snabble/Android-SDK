package io.snabble.sdk.ui.payment.externalbilling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.externalbilling.data.ExternalBillingPaymentCredentials
import io.snabble.sdk.payment.externalbilling.ExternalBillingRepositoryImpl
import io.snabble.sdk.ui.telemetry.Telemetry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ExternalBillingViewModel : ViewModel() {

    fun login(username: String, password: String): Boolean {
        viewModelScope.launch {
            val project = Snabble.checkedInProject.value
            project?.let {
                val repo = ExternalBillingRepositoryImpl(
                    it,
                    Json { ignoreUnknownKeys = true }
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
                            it.id
                        )
                        val success = repo.addPaymentCredentials(paymentCredentials)
                        if (success && paymentCredentials != null) {
                            trackCredentialType(paymentCredentials.type.name)
                        }
                    }
                }
            }
        }
        return false
    }

    private fun trackCredentialType(type: String) {
        Telemetry.event(Telemetry.Event.PaymentMethodAdded, type)
    }
}
