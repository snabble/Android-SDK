package io.snabble.sdk.coupons

import android.graphics.Color
import android.os.Parcelable
import android.util.DisplayMetrics
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.ColorUtils.parseColor
import io.snabble.sdk.Snabble
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
data class Coupon(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("promotionDescription") val promotionDescription: String?,
    @SerializedName("type") val type: CouponType,
    @SerializedName("codes") val codes: List<CouponCode>?,
    @SerializedName("code") val code: String?,
    @SerializedName("validFrom") private val _validFrom: String?,
    @SerializedName("validUntil") private val _validUntil: String?,
    @SerializedName("image") val image: CouponImage?,
    @SerializedName("disclaimer") val disclaimer: String?,
    @SerializedName("colors") val colors: Map<String, String>?,
) : Parcelable {

    val isValid: Boolean
        get() = when (type) {
            CouponType.DIGITAL -> image != null
            CouponType.MANUAL,
            CouponType.PRINTED -> true
        }

    @IgnoredOnParcel
    val validFrom: ZonedDateTime?
        get() = _validFrom?.let { ZonedDateTime.parse(_validFrom) }

    @IgnoredOnParcel
    val validUntil: ZonedDateTime?
        get() = _validUntil?.let { ZonedDateTime.parse(_validUntil) }

    @IgnoredOnParcel
    var isRedeemed: Boolean = false

    @IgnoredOnParcel
    val backgroundColor
        get() = parseColor(colors?.get("background"), Color.WHITE)

    @IgnoredOnParcel
    val textColor
        get() = parseColor(colors?.get("foreground"), Color.BLACK)
}

@Parcelize
data class CouponCode(
    @SerializedName("code") val code: String,
    @SerializedName("template") val template: String,
) : Parcelable

@Parcelize
data class CouponImage(
    @SerializedName("name") val name: String?,
    @SerializedName("formats") val formats: List<CouponImageFormats>,
) : Parcelable {

    val bestResolutionUrl: String
        get() {
            val res = Snabble.application.resources

            val mdpiRange = 0..DisplayMetrics.DENSITY_MEDIUM
            val hdpiRange = DisplayMetrics.DENSITY_MEDIUM..DisplayMetrics.DENSITY_HIGH
            val xhdpiRange = DisplayMetrics.DENSITY_HIGH..DisplayMetrics.DENSITY_XHIGH
            val xxhdpiRange = DisplayMetrics.DENSITY_XHIGH..DisplayMetrics.DENSITY_XXHIGH
            val xxxhdpiRange = DisplayMetrics.DENSITY_XXXHIGH..Int.MAX_VALUE

            val preferredDpi = when (res.displayMetrics.densityDpi) {
                in mdpiRange -> "mdpi"
                in hdpiRange -> "hdpi"
                in xhdpiRange -> "xhdpi"
                in xxhdpiRange -> "xxhdpi"
                in xxxhdpiRange -> "xxxhdpi"
                else -> null
            }

            val image = formats
                .filter { it.contentType == "image/webp" }
                .firstOrNull { it.size == preferredDpi }
                ?: this.formats.last()

            return image.url
        }
}

@Parcelize
data class CouponImageFormats(
    @SerializedName("contentType") val contentType: String,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("size") val size: String,
    @SerializedName("url") val url: String,
) : Parcelable

enum class CouponType {
    @SerializedName("manual")
    MANUAL,
    @SerializedName("printed")
    PRINTED,
    @SerializedName("digital")
    DIGITAL,
}
