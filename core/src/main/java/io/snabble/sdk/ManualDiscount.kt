package io.snabble.sdk

import java.math.BigDecimal

data class ManualDiscount(
    val label: String,
    val value: BigDecimal,
    val couponId: String
)