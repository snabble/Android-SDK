package io.snabble.sdk.ui.payment.fiserv.domain

data class CustomerInfo(
    val name: String,
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
