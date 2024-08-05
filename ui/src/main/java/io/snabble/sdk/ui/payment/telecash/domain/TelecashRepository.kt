package io.snabble.sdk.ui.payment.telecash.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.data.CreditCardAdditionInfo

internal interface TelecashRepository {

    suspend fun sendUserData(userDetails: UserDetails, paymentMethod: PaymentMethod): Result<CreditCardAdditionInfo>
}
