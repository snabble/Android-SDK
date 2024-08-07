package io.snabble.sdk.ui.payment.creditcard.fiserv.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.FiservCardRegisterUrls
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CustomerInfo

internal interface FiservRepository {

    suspend fun sendUserData(customerInfo: CustomerInfo, paymentMethod: PaymentMethod): Result<FiservCardRegisterUrls>
}
