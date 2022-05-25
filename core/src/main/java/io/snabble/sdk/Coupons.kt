package io.snabble.sdk

import android.os.Looper
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.lifecycle.LiveData
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Data class for a Coupon
 */
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
    val image: CouponImage?,
    val disclaimer: String?,
    val colors: Map<String, String>?,
) : Parcelable {
    val isValid: Boolean
    get() = when(type) {
        CouponType.DIGITAL -> image != null
        CouponType.MANUAL -> name != null
        CouponType.PRINTED -> true
    }
}

/**
 * Data class for a CouponCode
 */
@Parcelize
data class CouponCode (
    val code: String,
    val template: String,
) : Parcelable

/**
 * Data class for a CouponImage
 */
@Parcelize
data class CouponImage (
    val name: String,
    val formats: List<CouponImageFormats>,
) : Parcelable

/**
 * Data class for a CouponImageFormats
 */
@Parcelize
data class CouponImageFormats (
    val contentType: String,
    val width: Int?,
    val height: Int?,
    val size: String,
    val url: String,
) : Parcelable

/**
 * Enum class for a CouponType
 */
enum class CouponType {
    @SerializedName("manual") MANUAL,
    @SerializedName("printed") PRINTED,
    @SerializedName("digital") DIGITAL,
}

/**
 * Data class for a CouponSource
 */
enum class CouponSource {
    Bundled,
    Online,
}

/**
 * Coupon live data..
 * Can be iterated and filtered to get the current coupons on a project
 */
class Coupons (
    private val project: Project
) : Iterable<Coupon>, LiveData<List<Coupon>>() {
    val source: LiveData<CouponSource> = MutableAccessibleLiveData(CouponSource.Bundled)
    val isLoading: LiveData<Boolean> = MutableAccessibleLiveData(false)

    /**
     * Filter coupons based on the given coupon type
     */
    fun filter(type: CouponType): List<Coupon> =
        value?.filter { it.type == type } ?: emptyList()

    /**
     * Get all coupons
     */
    fun get(): List<Coupon>? = value

    operator fun get(i: Int) = value?.getOrNull(i) ?: throw ArrayIndexOutOfBoundsException()

    override fun iterator() = (value ?: emptyList()).iterator()

    /**
     * The size of the coupon list
     */
    val size: Int
        get() = value?.size ?: 0

    /**
     * Fetches new coupons from the backend. Multiple calls simultaneously are ignored until the
     * first call is done.
     */
    fun update() {
        if (isLoading.value == true) return

        fun <T> LiveData<T>.set(value: T) {
            (this as MutableAccessibleLiveData<T>).value = value
        }

        project.urls["coupons"]?.let { path ->
            val couponsUrl = Snabble.absoluteUrl(path)
            isLoading.set(true)

            val request = Request.Builder()
                .url(couponsUrl)
                .build()
            val couponCall = project.okHttpClient.newCall(request)
            couponCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    isLoading.set(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val localizedResponse = GsonBuilder().create()
                            .fromJson(response.body?.string(), CouponResponse::class.java)
                        postValue(localizedResponse.coupons.filter { coupon ->
                            coupon.isValid
                        })
                        source.set(CouponSource.Online)
                    }
                    isLoading.set(false)
                }
            })
        }
    }

    // Visibility for Project class. Used for setting the data asap if on main thread
    @JvmName("setInternalProjectCoupons")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun setProjectCoupons(coupons: List<Coupon>) {
        if (Looper.getMainLooper().thread.id == Thread.currentThread().id) {
            value = coupons
        } else {
            postValue(coupons)
        }
    }

    @Keep
    private data class CouponResponse(
        val coupons: List<Coupon>
    )
}