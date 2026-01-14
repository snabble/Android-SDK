package io.snabble.sdk.coupons

import android.os.Looper
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.lifecycle.LiveData
import com.google.gson.GsonBuilder
import io.snabble.sdk.MutableAccessibleLiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.compatId
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

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
                        val responseBody = response.body.string()
                        val localizedResponse = GsonBuilder().create()
                            .fromJson(responseBody, CouponResponse::class.java)
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
        if (Looper.getMainLooper().thread.compatId() == Thread.currentThread().compatId()) {
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
