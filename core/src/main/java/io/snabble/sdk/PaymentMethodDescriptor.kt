package io.snabble.sdk

data class PaymentMethodDescriptor(
    val id: String,
    val links: Map<String, Link>?,
    val providerName: String,
    val acceptedOriginTypes: List<String>
) {
    val paymentMethod: PaymentMethod
        get() {
            return PaymentMethod.fromString(id)!!
        }
}

data class Link(
    val href: String
)
