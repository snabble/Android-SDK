package io.snabble.sdk.ui.payment.creditcard.datatrans.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model.AuthData
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo

internal interface DatatransRepository {

    suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod,
        projectId: String
    ): Result<AuthData>
}
