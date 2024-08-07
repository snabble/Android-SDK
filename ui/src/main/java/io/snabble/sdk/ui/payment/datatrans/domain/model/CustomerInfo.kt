package io.snabble.sdk.ui.payment.datatrans.domain.model

internal data class CustomerInfo(
    val name: String,
    val email: String,
    val address: Address,
    val countryCode: String,
    val subscriber: String,
)

internal data class Address(
    val street: String,
    val zip: String,
    val city: String,
    val state: String?,
    val country: String,
)
