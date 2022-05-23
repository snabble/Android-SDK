package io.snabble.sdk

/**
 * A class describing a payment method
 */
data class PaymentMethodDescriptor(
    /**
     * Unique identifier of the payment method
     */
    val id: String,
    /**
     * Various links for getting web forms or approval links
     */
    val links: Map<String, Link>?,
    /**
     * The name of the underlying payment provider (e.g. telecash, payOne, googlePay)
     */
    val providerName: String,
    /**
     * List of origin types that are accepted for this payment method (to match with locally saved
     * payment credentials)
     */
    val acceptedOriginTypes: List<String>?
) {
    /**
     * Convert this descriptor to the SDK [PaymentMethod]
     */
    val paymentMethod: PaymentMethod
        get() = PaymentMethod.fromIdAndOrigin(id, acceptedOriginTypes)!!
}

/**
 * POJO for a link
 */
data class Link(
    val href: String
)
