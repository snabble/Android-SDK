package io.snabble.sdk.ui.checkout

import io.snabble.sdk.checkout.Checkout

val Checkout.State.isCheckoutState: Boolean
    get() = when (this) {
        Checkout.State.NONE,
        Checkout.State.HANDSHAKING,
        Checkout.State.REQUEST_PAYMENT_METHOD,
        Checkout.State.VERIFYING_PAYMENT_METHOD,
        Checkout.State.REQUEST_VERIFY_AGE,
        Checkout.State.REQUEST_TAXATION,
        Checkout.State.CONNECTION_ERROR,
        Checkout.State.INVALID_PRODUCTS,
        Checkout.State.NO_PAYMENT_METHOD_AVAILABLE,
        Checkout.State.NO_SHOP -> false

        Checkout.State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN,
        Checkout.State.WAIT_FOR_SUPERVISOR,
        Checkout.State.WAIT_FOR_GATEKEEPER,
        Checkout.State.WAIT_FOR_APPROVAL,
        Checkout.State.PAYMENT_PROCESSING,
        Checkout.State.PAYMENT_APPROVED,
        Checkout.State.DENIED_TOO_YOUNG,
        Checkout.State.DENIED_BY_PAYMENT_PROVIDER,
        Checkout.State.DENIED_BY_SUPERVISOR,
        Checkout.State.PAYMENT_ABORTED,
        Checkout.State.PAYMENT_ABORT_FAILED,
        Checkout.State.PAYMENT_PROCESSING_ERROR -> true
    }