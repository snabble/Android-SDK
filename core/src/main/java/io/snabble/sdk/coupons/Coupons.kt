package io.snabble.sdk.coupons

import android.os.Looper
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import com.google.gson.GsonBuilder
import io.snabble.sdk.MutableAccessibleLiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

enum class CouponSource {
    Bundled,
    Online,
}

class Coupons (
    private val project: Project
) : Iterable<Coupon>, LiveData<List<Coupon>>() {
    val source: LiveData<CouponSource> = MutableAccessibleLiveData(CouponSource.Bundled)
    val isLoading: LiveData<Boolean> = MutableAccessibleLiveData(false)

    fun filter(type: CouponType): List<Coupon> =
        value?.filter { it.type == type } ?: emptyList()

    fun get(): List<Coupon>? = value

    operator fun get(i: Int) = value?.getOrNull(i) ?: throw ArrayIndexOutOfBoundsException()

    override fun iterator() = (value ?: emptyList()).iterator()

    val size: Int
        get() = value?.size ?: 0

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
                        val response = response.body?.string()
                        val localizedResponse = GsonBuilder().create()
                            .fromJson(response, CouponResponse::class.java)
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