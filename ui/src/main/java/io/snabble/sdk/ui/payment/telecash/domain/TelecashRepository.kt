package io.snabble.sdk.ui.payment.telecash.domain

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.data.PreAuthInformation

interface TelecashRepository {

    suspend fun preAuth(userDetails: UserDetails, paymentMethod: PaymentMethod): Result<PreAuthInformation?>?
}
