package io.snabble.sdk.ui.coupon

import android.content.res.Resources
import android.graphics.Color
import android.os.Parcelable
import android.util.DisplayMetrics
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import io.snabble.sdk.CouponImage
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.I18nUtils.getQuantityStringForProject
import io.snabble.sdk.ui.utils.I18nUtils.getStringForProject
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Keep
@Parcelize
data class Coupon(
    val subtitle: String,
    val code: String,
    val id: String,
    val title: String,
    val validFrom: ZonedDateTime?,
    val text: String,
    val validUntil: ZonedDateTime?,
    val imageURL: String?,
    @ColorInt val backgroundColor: Int,
    @ColorInt val textColor: Int,
    val disclaimer: String,
    val mode: Mode = Mode.Normal,
    private val projectId: String?,
): Parcelable {
    constructor(project: Project, sdkCoupon: io.snabble.sdk.Coupon): this(
        subtitle = sdkCoupon.description.orEmpty(),
        code = sdkCoupon.code.orEmpty(),
        id = sdkCoupon.id,
        title = sdkCoupon.name.orEmpty(),
        validFrom = sdkCoupon.validFrom?.let { ZonedDateTime.parse(it) },
        text = sdkCoupon.promotionDescription.orEmpty(),
        validUntil = sdkCoupon.validUntil?.let { ZonedDateTime.parse(it) },
        imageURL = sdkCoupon.image?.bestResolutionUrl,
        backgroundColor = parseColor(sdkCoupon.colors?.get("background"), Color.WHITE),
        textColor = parseColor(sdkCoupon.colors?.get("foreground"), Color.BLACK),
        disclaimer = sdkCoupon.disclaimer.orEmpty(),
        projectId = project.id
    )

    fun buildExpireString(resources: Resources): String =
        if (validUntil == null || validFrom == null) {
            ""
        } else {
            when (val expire = validUntil.toEpochSecond() * 1000 - System.currentTimeMillis()) {
                in past -> {
                    resources.getStringForProject(projectId, R.string.Snabble_Coupons_expired)
                }
                in minutes -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(expire).toInt()
                    resources.getQuantityStringForProject(
                        projectId,
                        R.plurals.Snabble_Coupons_expiresInMinutes,
                        minutes,
                        minutes
                    )
                }
                in hours -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(expire).toInt()
                    resources.getQuantityStringForProject(projectId, R.plurals.Snabble_Coupons_expiresInHours, hours, hours)
                }
                in days -> {
                    val days = TimeUnit.MILLISECONDS.toDays(expire).toInt()
                    resources.getQuantityStringForProject(projectId, R.plurals.Snabble_Coupons_expiresInDays, days, days)
                }
                in weeks -> {
                    val weeks = TimeUnit.MILLISECONDS.toDays(expire).toInt() / 7
                    resources.getQuantityStringForProject(projectId, R.plurals.Snabble_Coupons_expiresInWeeks, weeks, weeks)
                }
                else -> resources.getStringForProject(
                    projectId,
                    R.string.Snabble_Coupons_expiresAtDate,
                    SimpleDateFormat.getDateInstance().format(Date(validUntil.toEpochSecond() * 1000))
                )
            }
        }

    companion object {
        private val hour: Long = TimeUnit.HOURS.toMillis(1)
        private val day: Long = TimeUnit.DAYS.toMillis(1)
        private val week: Long = TimeUnit.DAYS.toMillis(7)
        private val past = Long.MIN_VALUE..0
        private val minutes = 0..hour
        private val hours = hour..day
        private val days = day..week
        private val weeks = week..Long.MAX_VALUE

        fun parseColor(color: String?, @ColorInt default: Int) =
            color?.let {
                Color.parseColor(when {
                    "^[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?$".toRegex().matches(color) -> {
                        // add missing prefix
                        "#$color"
                    }
                    "^#?[0-9a-fA-F]{3}$".toRegex().matches(color) -> {
                        // convert 3 digit color to 6 digits
                        color.removePrefix("#").toCharArray()
                            .joinToString(separator = "", prefix = "#") { "$it$it" }
                    }
                    else -> {
                        color
                    }
                })
            } ?: default

        fun createLoadingPlaceholder() = Coupon("", "", "", "", ZonedDateTime.now(), "", ZonedDateTime.now(), "", 0,0,"", Mode.Loading, null)
        fun createEmptyPlaceholder() = Coupon("", "", "", "", ZonedDateTime.now(), "", ZonedDateTime.now(), "", 0,0,"", Mode.Empty, null)
    }

    enum class Mode {
        Loading,
        Normal,
        Empty,
    }
}

val CouponImage.bestResolutionUrl: String
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