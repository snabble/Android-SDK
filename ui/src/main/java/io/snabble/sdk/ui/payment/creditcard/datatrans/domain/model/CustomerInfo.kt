package io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model

internal data class CustomerInfo(
    val name: String,
    val email: String,
    val address: Address,
    val intCallingCode: String,
    val phoneNumber: String,
)

internal data class Address(
    val street: String,
    val zip: String,
    val city: String,
    val state: String?,
    val country: String,
)
