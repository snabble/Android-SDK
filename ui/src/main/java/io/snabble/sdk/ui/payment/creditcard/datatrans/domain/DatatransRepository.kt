package io.snabble.sdk.ui.payment.creditcard.datatrans.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model.DatatransTokenizationResponse
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CustomerInfo

internal interface DatatransRepository {

    suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<DatatransTokenizationResponse>
}
