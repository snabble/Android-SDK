package io.snabble.sdk.shoppingcart.data.item

import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.coupons.Coupon

data class CouponItem(
    val coupon: Coupon,
    val scannedCode: ScannedCode
)
