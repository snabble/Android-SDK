package io.snabble.sdk

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coupon (
    val id: String,
    val name: String?,
    val description: String?,
    val promotionDescription: String?,
    val type: CouponType,
    val codes: List<CouponCode>?,
    val code: String?,
    val validFrom: String?,
    val validUntil: String?,
    val image: CouponImage?
) : Parcelable

@Parcelize
data class CouponCode (
    val code: String,
    val template: String,
) : Parcelable

@Parcelize
data class CouponImage (
    val name: String,
    val formats: List<CouponImageFormats>,
) : Parcelable

@Parcelize
data class CouponImageFormats (
    val contentType: String,
    val width: Int,
    val height: Int,
    val size: String,
    val url: String,
) : Parcelable

enum class CouponType {
    @SerializedName("manual") MANUAL,
    @SerializedName("printed") PRINTED,
    @SerializedName("digital") DIGITAL,
}

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