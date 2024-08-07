package io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto

internal data class DatatransTokenizationResponseDto(
    val mobileToken: String,
    val isTesting: Boolean = false,
)
