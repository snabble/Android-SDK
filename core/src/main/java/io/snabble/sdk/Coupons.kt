package io.snabble.sdk

import com.google.gson.annotations.SerializedName

data class Coupon (
    val id: String,
    val name: String,
    val type: CouponType,
    val codes: List<CouponCode>,
)

data class CouponCode (
    val code: String,
    val template: String,
)

enum class CouponType {
    @SerializedName("manual") MANUAL,
    @SerializedName("printed") PRINTED,
    @SerializedName("digital") DIGITAL,
}

data class PersistedCouponData (
    val activatedCouponIds: MutableList<String> = mutableListOf(),
    val printedCouponsIds: MutableList<String> = mutableListOf(),
)

class Coupons (
    private val coupons: List<Coupon>,
) {
    @JvmOverloads
    fun get(type: CouponType? = null): List<Coupon> {
        if (type == null) {
            return coupons
        }

        return coupons.filter { it.type == type }
    }
}