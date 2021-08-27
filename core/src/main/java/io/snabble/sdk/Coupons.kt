package io.snabble.sdk

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

enum class CouponSource {
    Bundled,
    Online,
}

class Coupons (
    coupons: List<Coupon>,
    private val project: Project
) : Iterable<Coupon>, LiveData<List<Coupon>>(coupons) {
    init {
        project.addOnUpdateListener {
            if (source.value == CouponSource.Bundled) {
                postValue(project.coupons.value)
            }
        }
        update()
    }

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
        if(isLoading.value == true) return
        // make an implicit cast with an extension function
        fun <T> LiveData<T>.postValue(value: T) {
            (this as? MutableLiveData<T>)?.postValue(value)
        }
        project.urls["coupons"]?.let { path ->
            isLoading.postValue(true)
            val newsUrl = Snabble.getInstance().absoluteUrl(path)

            if (newsUrl == null) {
                postValue(emptyList())
                return
            }

            val request = Request.Builder()
                    .url(newsUrl)
                    .build()
            val couponCall = project.okHttpClient.newCall(request)
            couponCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    postValue(emptyList())
                    isLoading.postValue(false)
                    source.postValue(CouponSource.Online)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val localizedResponse = GsonBuilder().create()
                                .fromJson(response.body?.string(), CouponResponse::class.java)
                        postValue(localizedResponse.coupons.filter {
                            it.image != null && it.validFrom != null && it.validUntil != null
                        })
                    } else {
                        postValue(emptyList())
                    }
                    isLoading.postValue(false)
                    source.postValue(CouponSource.Online)
                }
            })
        }
    }

    @Keep
    private data class CouponResponse(
            val coupons: List<Coupon>
    )
}