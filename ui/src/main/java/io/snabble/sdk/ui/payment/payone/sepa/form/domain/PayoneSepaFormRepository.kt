package io.snabble.sdk.ui.payment.payone.sepa.form.domain

import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import io.snabble.sdk.ui.telemetry.Telemetry

interface PayoneSepaFormRepository {

    /**
     * True if the PayoneSepaData has been saved, false otherwise.
     */
    fun saveSepaData(data: PayoneSepaData): Boolean
}

class PayoneSepaFormRepositoryImpl(
    private val snabble: Snabble,
) : PayoneSepaFormRepository {

    override fun saveSepaData(data: PayoneSepaData): Boolean {
        val paymentCredentials = PaymentCredentials.fromPayoneSepa(data) ?: return false

        snabble.paymentCredentialsStore.add(paymentCredentials)
        val type = paymentCredentials.type
        if (type != null) {
            trackCredentialType(type.name)
        }
        return true
    }

    private fun trackCredentialType(type: String) {
        Telemetry.event(Telemetry.Event.PaymentMethodAdded, type)
    }
}
