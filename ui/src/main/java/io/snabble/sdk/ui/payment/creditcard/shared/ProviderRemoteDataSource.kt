package io.snabble.sdk.ui.payment.creditcard.shared

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal abstract class ProviderRemoteDataSource<T>(private val gson: Gson) {

    internal suspend fun OkHttpClient.post(request: Request) = suspendCoroutine<Result<T>> {
        newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                when {
                    response.isSuccessful -> {
                        val body = response.body?.string()
                        val typeToken = object : TypeToken<T>() {}.type
                        val data: T? = try {
                            gson.fromJson(body, typeToken)
                        } catch (e: JsonSyntaxException) {
                            Log.e("Payment", "Error parsing pre-registration response", e)
                            null
                        }

                        val result = if (data == null) {
                            Result.failure(Exception("Missing content"))
                        } else {
                            Result.success(data)
                        }
                        it.resume(result)
                    }

                    else -> it.resume(Result.failure(Exception(response.message)))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                it.resume(Result.failure(e))
            }
        })
    }
}
