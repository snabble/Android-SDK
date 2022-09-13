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
data class Coupon (
    val id: String,
    val name: String,
    val description: String?,
    val promotionDescription: String?,
    val type: CouponType,
    val codes: List<CouponCode>?,
    val code: String?,
    @SerializedName("validFrom")
    private val _validFrom: String?,
    @SerializedName("validUntil")
    private val _validUntil: String?,
    val image: CouponImage?,
    val disclaimer: String?,
    val colors: Map<String, String>?,
) : Parcelable {
    val isValid: Boolean
    get() = when(type) {
        CouponType.DIGITAL -> image != null
        CouponType.MANUAL,
        CouponType.PRINTED -> true
    }

    @IgnoredOnParcel
    val validFrom: ZonedDateTime?
        get() = _validFrom?.let { ZonedDateTime.parse(_validFrom)}

    @IgnoredOnParcel
    val validUntil: ZonedDateTime?
        get() = _validUntil?.let { ZonedDateTime.parse(_validUntil)}

    @IgnoredOnParcel
    val backgroundColor
        get() = parseColor(colors?.get("background"), Color.WHITE)

    @IgnoredOnParcel
    val textColor
        get() = parseColor(colors?.get("foreground"), Color.BLACK)
}

@Parcelize
data class CouponCode (
    val code: String,
    val template: String,
) : Parcelable

@Parcelize
data class CouponImage (
    val name: String?,
    val formats: List<CouponImageFormats>,
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
data class CouponImageFormats (
    val contentType: String,
    val width: Int?,
    val height: Int?,
    val size: String,
    val url: String,
) : Parcelable

enum class CouponType {
    @SerializedName("manual") MANUAL,
    @SerializedName("printed") PRINTED,
    @SerializedName("digital") DIGITAL,
}
