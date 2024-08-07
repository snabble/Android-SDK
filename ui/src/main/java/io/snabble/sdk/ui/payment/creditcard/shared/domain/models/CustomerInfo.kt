package io.snabble.sdk.ui.payment.creditcard.shared.domain.models

data class CustomerInfo(
    val name: String,
    val intCallingCode: String,
    val phoneNumber: String,
    val email: String,
    val address: Address,
)

data class Address(
    val street: String,
    val zip: String,
    val city: String,
    val state: String,
    val country: String
)
