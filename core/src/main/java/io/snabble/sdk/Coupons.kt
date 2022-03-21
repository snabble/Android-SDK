package io.snabble.sdk

import android.os.Looper
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

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

enum class CouponSource {
    Bundled,
    Online,
}

class Coupons (
    private val project: Project
) : Iterable<Coupon>, LiveData<List<Coupon>>() {
    val source: LiveData<CouponSource> = MutableLiveData(CouponSource.Bundled)
    val isLoading: LiveData<Boolean> = MutableLiveData(false)

    fun filter(type: CouponType): List<Coupon> =
        value?.filter { it.type == type } ?: emptyList()

    fun get(): List<Coupon>? = value

    operator fun get(i: Int) = value?.getOrNull(i) ?: throw ArrayIndexOutOfBoundsException()

    override fun iterator() = (value ?: emptyList()).iterator()

    val size: Int
        get() = value?.size ?: 0

    fun update() {
        if (isLoading.value == true) return
        fun <T> LiveData<T>.setAsap(value: T) {
            if (Looper.getMainLooper().thread.id == Thread.currentThread().id) {
                (this as MutableLiveData<T>).value = value
            } else {
                (this as MutableLiveData<T>).postValue(value)
            }
        }
        project.urls["coupons"]?.let { path ->
            val couponsUrl = Snabble.absoluteUrl(path)
            isLoading.setAsap(true)

            val request = Request.Builder()
                .url(couponsUrl)
                .build()
            val couponCall = project.okHttpClient.newCall(request)
            couponCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    isLoading.setAsap(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val localizedResponse = GsonBuilder().create()
                            .fromJson(response.body?.string(), CouponResponse::class.java)
                        postValue(localizedResponse.coupons.filter { coupon ->
                            coupon.isValid
                        })
                        source.setAsap(CouponSource.Online)
                    }
                    isLoading.setAsap(false)
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