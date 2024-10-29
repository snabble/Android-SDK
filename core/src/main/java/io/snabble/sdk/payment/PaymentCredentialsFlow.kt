@file:JvmName("PaymentCredentialsFlow")

package io.snabble.sdk.payment

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal interface MutableCredentialsFlow {

    fun tryEmitCredentials(credentials: PaymentCredentials): Boolean
}

/**
 * This flow is emitting payment credentials if adding a payment method has been configured to be added without being
 * saved.
 *
 * Payment method being added the regular way, won't be emitted here.
 * The [PaymentCredentialsStore.OnPaymentCredentialsAddedListener] is called in that case.
 */
object PaymentCredentialsFlow : MutableCredentialsFlow {

    private val _credentialsFlow = MutableSharedFlow<PaymentCredentials?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Collect this flow to be notified if payment credentials has been created successfully.
     * If there are no collectors, it will be discarded.
     */
    val credentialsFlow: SharedFlow<PaymentCredentials?> = _credentialsFlow.asSharedFlow()

    override fun tryEmitCredentials(credentials: PaymentCredentials): Boolean = _credentialsFlow.tryEmit(credentials)
}
