package io.snabble.sdk.ui.coupon

import android.content.res.Resources
import android.graphics.Color
import android.os.Parcelable
import android.util.DisplayMetrics
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import io.snabble.sdk.coupons.CouponImage
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.I18nUtils.getQuantityStringForProject
import io.snabble.sdk.ui.utils.I18nUtils.getStringForProject
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@Keep
@Parcelize
data class CouponItem(
    val projectId: String?,
    val coupon: Coupon?,
    val mode: Mode = Mode.Normal,
): Parcelable {
    fun buildExpireString(resources: Resources): String? =
        if (coupon?.validUntil == null || coupon.validFrom == null) {
            null
        } else {
            when (val expire = (coupon.validUntil?.toEpochSecond() ?: 0) * 1000 - System.currentTimeMillis()) {
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
                    coupon.validUntil?.format(DateTimeFormatter.ISO_LOCAL_DATE),
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

        fun createLoadingPlaceholder() = CouponItem(null, null, Mode.Loading)
        fun createEmptyPlaceholder() = CouponItem(null, null, Mode.Empty)
    }

    enum class Mode {
        Loading,
        Normal,
        Empty,
    }
}
