package io.snabble.sdk

import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import okhttp3.Request

data class Coupon (
    val id: String,
    val name: String,
    val type: String,
    val codes: List<CouponCode>,
)

data class CouponCode (
    val code: String,
    val template: String,
)

private data class CouponsJson (
    val coupons: List<Coupon>
)

interface CouponUpdateCallback {
    fun success(coupons: List<Coupon>)
    fun failure()
}

class CouponApi(
    val project: Project
) {
    fun get(): List<Coupon>? {
        val url = project.couponsUrl
        if (url != null) {
            val call = project.okHttpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()
            )

            try {
                call.execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.use { body ->
                            return GsonHolder.get().fromJson(body.string(), CouponsJson::class.java).coupons
                        }
                    }
                }
            } catch (e: Exception) {
                return null
            }
         }

        return null
    }

    fun getAsync(callback: CouponUpdateCallback) {
        Dispatch.background {
            val coupons = get()

            if (coupons != null) {
                callback.success(coupons)
            } else {
                callback.failure()
            }
        }
    }
}