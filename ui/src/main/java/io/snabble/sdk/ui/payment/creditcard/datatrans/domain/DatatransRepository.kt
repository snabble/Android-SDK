package io.snabble.sdk.ui.payment.creditcard.datatrans.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model.CustomerInfo
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model.DatatransTokenizationResponse

internal interface DatatransRepository {

    suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<DatatransTokenizationResponse>
}
