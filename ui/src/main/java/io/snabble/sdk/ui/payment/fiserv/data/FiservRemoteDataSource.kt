package io.snabble.sdk.ui.payment.fiserv.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.GsonHolder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal interface FiservRemoteDataSource {

    suspend fun sendUserData(customerInfo: CustomerInfoDto, paymentMethod: PaymentMethod): Result<CreditCardAuthData>
}

internal class FiservRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : FiservRemoteDataSource {

    override suspend fun sendUserData(
        customerInfo: CustomerInfoDto,
        paymentMethod: PaymentMethod
    ): Result<CreditCardAuthData> {
        val project = snabble.checkedInProject.value ?: return Result.failure(Exception("Missing projectId"))

        val customerInfoPostUrl = project.paymentMethodDescriptors
            .firstOrNull { it.paymentMethod == paymentMethod }
            ?.links
            ?.get("tokenization")
            ?.href
            ?.let(snabble::absoluteUrl)
            ?: return Result.failure(Exception("Missing link to send customer info to"))

        val requestBody: RequestBody = gson.toJson(customerInfo).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(customerInfoPostUrl)
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request)
    }

    private suspend fun OkHttpClient.post(request: Request) = suspendCoroutine<Result<CreditCardAuthData>> {
        newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                when {
                    response.isSuccessful -> {
                        val body = response.body?.string()
                        val creditCardAuthData: CreditCardAuthData? = try {
                            gson.fromJson(body, CreditCardAuthData::class.java)
                        } catch (e: JsonSyntaxException) {
                            Log.e("Fiserv", "Error parsing pre-registration response", e)
                            null
                        }

                        val result = if (creditCardAuthData == null) {
                            Result.failure(Exception("Missing content"))
                        } else {
                            Result.success(creditCardAuthData)
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

internal data class CreditCardAuthData(
    @SerializedName("links") val links: Links
)

internal data class Links(
    @SerializedName("self") val deleteUrl: Link,
    @SerializedName("tokenizationForm") val formUrl: Link
)

internal data class Link(
    @SerializedName("href") val href: String
)
