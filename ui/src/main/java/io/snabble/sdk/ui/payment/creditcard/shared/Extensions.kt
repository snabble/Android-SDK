package io.snabble.sdk.ui.payment.creditcard.shared

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.PaymentMethodDescriptor
import io.snabble.sdk.Snabble
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal fun List<PaymentMethodDescriptor>.getTokenizationUrlFor(paymentMethod: PaymentMethod): String? =
    firstOrNull { it.paymentMethod == paymentMethod }
        ?.links
        ?.get("tokenization")
        ?.href
        ?.let(Snabble::absoluteUrl)

internal suspend inline fun <reified T> OkHttpClient.post(request: Request, gson: Gson) = suspendCoroutine<Result<T>> {
    newCall(request).enqueue(object : Callback {

        override fun onResponse(call: Call, response: Response) {
            when {
                response.isSuccessful -> {
                    val body = response.body?.string()
                    val typeToken = object : TypeToken<T>() {}.type
                    val data: T? = try {
                        gson.fromJson<T>(body, typeToken)
                    } catch (e: JsonSyntaxException) {
                        Log.e("Payment", "Error parsing pre-registration response", e)
                        null
                    }

                    val result = if (data == null) {
                        Log.e("Payment", body ?: "Unkown cause")
                        Result.failure(Exception("Missing content"))
                    } else {
                        Result.success(data)
                    }
                    it.resume(result)
                }

                else -> {
                    val body = response.body?.string()
                    Log.e("Payment", body ?: "Unkown cause")
                    it.resume(Result.failure(Exception(response.message)))
                }
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("Payment", e.localizedMessage ?: "Unkown cause")
            it.resume(Result.failure(e))
        }
    })
}
