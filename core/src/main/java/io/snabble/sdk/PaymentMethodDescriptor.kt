package io.snabble.sdk

data class PaymentMethodDescriptor(
    val id: String,
    val links: Map<String, Link>?,
    val providerName: String,
    val acceptedOriginTypes: List<String>?
) {
    val paymentMethod: PaymentMethod
        get() = PaymentMethod.fromIdAndOrigin(id, acceptedOriginTypes)!!
}

data class Link(
    val href: String
)
