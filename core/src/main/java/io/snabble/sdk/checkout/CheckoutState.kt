package io.snabble.sdk.checkout

enum class CheckoutState {
    /**
     * The initial default state
     */
    NONE,

    /**
     * A checkout request was started, and we are waiting for the backend to confirm
     */
    HANDSHAKING,

    /**
     * The checkout request is started and confirmed by the backend. We are waiting for
     * selection of the payment method. Usually done by the user.
     *
     *
     * Gets skipped for projects that have only 1 available payment method,
     * that can be selected without user intervention.
     */
    REQUEST_PAYMENT_METHOD,

    /**
     * Payment method was selected and we are waiting for confirmation of the backend
     */
    VERIFYING_PAYMENT_METHOD,

    /**
     * Age needs to be verified
     */
    REQUEST_VERIFY_AGE,

    /**
     * Ask the user for the taxation method
     */
    REQUEST_TAXATION,

    /**
     * Request a payment authorization token.
     *
     * For example a Google Pay payment token that needs to get sent back to
     * the snabble Backend.
     */
    REQUEST_PAYMENT_AUTHORIZATION_TOKEN,

    /**
     * Checkout was received and we wait for confirmation by the supervisor
     */
    WAIT_FOR_SUPERVISOR,

    /**
     * Checkout was received and we wait for confirmation by the gatekeeper
     */
    WAIT_FOR_GATEKEEPER,

    /**
     * Payment was received by the backend and we are waiting for confirmation by the payment provider
     */
    WAIT_FOR_APPROVAL,

    /**
     * Payment was approved and is currently processing
     */
    PAYMENT_PROCESSING,

    /**
     * The payment was approved. We are done
     */
    PAYMENT_APPROVED,

    /**
     * Age is too young
     */
    DENIED_TOO_YOUNG,

    /**
     * The payment was denied by the payment provider
     */
    DENIED_BY_PAYMENT_PROVIDER,

    /**
     * The payment was denied by the supervisor
     */
    DENIED_BY_SUPERVISOR,

    /**
     * The payment was aborted
     */
    PAYMENT_ABORTED,

    /**
     * The payment could not be aborted
     */
    PAYMENT_ABORT_FAILED,

    /**
     * There was a unrecoverable payment processing error
     */
    PAYMENT_PROCESSING_ERROR,

    /**
     * There was a unrecoverable connection error
     */
    CONNECTION_ERROR,

    /**
     * Invalid products detected. For example if a sale stop was issued
     */
    INVALID_PRODUCTS,

    /**
     * Invalid items detected. For if an item can't be found
     */
    INVALID_ITEMS,

    /**
     * No payment method available
     */
    NO_PAYMENT_METHOD_AVAILABLE,

    /**
     * No shop was selected
     */
    NO_SHOP,

    /**
     * SEPA mandate for PAYONE is needed
     */
    PAYONE_SEPA_MANDATE_REQUIRED,

    /**
     * The process was transferred to a payment system. The outcome of the processing
     * will not be communicated
     */
    PAYMENT_TRANSFERRED,
}
