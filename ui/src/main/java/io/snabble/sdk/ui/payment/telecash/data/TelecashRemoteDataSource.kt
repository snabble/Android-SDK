package io.snabble.sdk.ui.payment.telecash.data

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.GsonHolder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface TelecashRemoteDataSource {

    suspend fun preAuth(userDetails: UserDetailsDto, paymentMethod: PaymentMethod): Result<PreAuthData>
}

class TelecashRemoteDataSourceImpl : TelecashRemoteDataSource {

    override suspend fun preAuth(userDetails: UserDetailsDto, paymentMethod: PaymentMethod): Result<PreAuthData> {
        val project = Snabble.checkedInProject.value ?: return Result.failure(Exception("Missing projectId"))
        val authUrl = project.paymentMethodDescriptors
            .firstOrNull { it.paymentMethod == paymentMethod }
            ?.links
            ?.get("tokenization")
            ?: return Result.failure(Exception("Missing pre-auth Url"))

        val requestBody = GsonHolder.get().toJson(userDetails).toRequestBody("application/json".toMediaType())

        val request: Request = Request.Builder()
            .url(Snabble.absoluteUrl(authUrl.href))
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request)
    }

    private suspend fun OkHttpClient.post(request: Request) = suspendCoroutine<Result<PreAuthData>> {
        newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                it.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                when {
                    response.isSuccessful -> {
                        val body = response.body?.string()
                        val preAuthData: PreAuthData? = GsonHolder.get().fromJson(body, PreAuthData::class.java)
                        if (preAuthData == null) {
                            it.resume(Result.failure(Exception("Missing content")))
                        } else {
                            it.resume(Result.success(preAuthData))
                        }
                    }

                    else -> {
                        it.resume(Result.failure(Exception(response.message)))
                    }
                }
            }
        })
    }
}

data class PreAuthData(
    @SerializedName("links") val links: Links
)

data class Links(
    @SerializedName("self") val deleteUrl: Link,
    @SerializedName("tokenizationForm") val formUrl: Link
)

data class Link(
    @SerializedName("href") val href: String
)
