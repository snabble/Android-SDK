package io.snabble.sdk.ui.payment.externalbilling

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.externalbilling.EncryptedBillingCredentials
import io.snabble.sdk.ui.payment.externalbilling.domain.ExternalBillingRepositoryImpl
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ExternalBillingViewModel : ViewModel() {

    fun login(username: String, password: String): Boolean {
       viewModelScope.launch {
            val project = Snabble.checkedInProject.value
            project?.let{
                val repo = ExternalBillingRepositoryImpl(
                    it,
                    Json { ignoreUnknownKeys = true }
                )
                val result = repo.login(username, password)
                if (result.isSuccess) {
                    val billingCredentials = result.getOrNull()
                    billingCredentials?.let { bc ->
                        val paymentCredentials: PaymentCredentials? = PaymentCredentials.fromExternalBilling(
                            EncryptedBillingCredentials(
                                username = username,
                                contactPersonID = bc.contactPersonID,
                                password = password
                            ),
                            it.id
                        )
                        repo.addPaymentCredentials(paymentCredentials)
                    }
                }
            }
        }
        return false
    }
}
