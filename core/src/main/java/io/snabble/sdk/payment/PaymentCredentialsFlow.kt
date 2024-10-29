@file:JvmName("PaymentCredentialsFlow")

package io.snabble.sdk.payment

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal interface MutableCredentialsFlow {

    fun emitCredentials(credentials: PaymentCredentials)
}

/**
 * This flow is emitting payment credentials if adding a payment method has been configured to be added without being
 * saved.
 *
 * Payment method being added the regular way, won't be emitted here.
 * The [PaymentCredentialsStore.OnPaymentCredentialsAddedListener] is called in that case.
 */
object PaymentCredentialsFlow : MutableCredentialsFlow {

    private val _credentialsFlow = MutableSharedFlow<PaymentCredentials?>()

    /**
     * Collect this flow to be notified if payment credentials has been created successfully.
     * If there are no collectors, it will be discarded.
     */
    val credentialsFlow: SharedFlow<PaymentCredentials?> = _credentialsFlow.asSharedFlow()

    override fun emitCredentials(credentials: PaymentCredentials) {
        MainScope().launch {
            _credentialsFlow.emit(credentials)
        }
    }
}
