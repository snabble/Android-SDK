package io.snabble.sdk.ui.checkout

import io.snabble.sdk.checkout.CheckoutState

val CheckoutState.isCheckoutState: Boolean
    get() = when (this) {
        CheckoutState.NONE,
        CheckoutState.HANDSHAKING,
        CheckoutState.REQUEST_PAYMENT_METHOD,
        CheckoutState.VERIFYING_PAYMENT_METHOD,
        CheckoutState.REQUEST_VERIFY_AGE,
        CheckoutState.REQUEST_TAXATION,
        CheckoutState.CONNECTION_ERROR,
        CheckoutState.INVALID_PRODUCTS,
        CheckoutState.NO_PAYMENT_METHOD_AVAILABLE,
        CheckoutState.PAYMENT_ABORTED,
        CheckoutState.NO_SHOP -> false

        CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN,
        CheckoutState.WAIT_FOR_SUPERVISOR,
        CheckoutState.WAIT_FOR_GATEKEEPER,
        CheckoutState.WAIT_FOR_APPROVAL,
        CheckoutState.PAYMENT_PROCESSING,
        CheckoutState.PAYMENT_APPROVED,
        CheckoutState.DENIED_TOO_YOUNG,
        CheckoutState.DENIED_BY_PAYMENT_PROVIDER,
        CheckoutState.DENIED_BY_SUPERVISOR,
        CheckoutState.PAYMENT_ABORT_FAILED,
        CheckoutState.PAYMENT_PROCESSING_ERROR,
        CheckoutState.PAYMENT_TRANSFERRED,
        CheckoutState.PAYONE_SEPA_MANDATE_REQUIRED -> true
    }
