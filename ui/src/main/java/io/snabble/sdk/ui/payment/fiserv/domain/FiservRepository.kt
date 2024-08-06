package io.snabble.sdk.ui.payment.fiserv.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.fiserv.data.FiservCardRegisterUrls

internal interface FiservRepository {

    suspend fun sendUserData(customerInfo: CustomerInfo, paymentMethod: PaymentMethod): Result<FiservCardRegisterUrls>
}
