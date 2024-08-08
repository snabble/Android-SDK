package io.snabble.sdk.ui.payment.creditcard.datatrans.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.DatatransTokenizationRequestDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.DatatransTokenizationResponseDto
import io.snabble.sdk.ui.payment.creditcard.shared.getTokenizationUrlFor
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

internal interface DatatransRemoteDataSource {

    suspend fun sendUserData(
        datatransTokenizationRequest: DatatransTokenizationRequestDto,
    ): Result<DatatransTokenizationResponseDto>
}

internal class DatatransRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : DatatransRemoteDataSource {

    override suspend fun sendUserData(
        datatransTokenizationRequest: DatatransTokenizationRequestDto,
    ): Result<DatatransTokenizationResponseDto> {
        val project = snabble.checkedInProject.value ?: return Result.failure(Exception("Missing projectId"))

        val customerInfoPostUrl =
            project.paymentMethodDescriptors.getTokenizationUrlFor(datatransTokenizationRequest.paymentMethod)
                ?: return Result.failure(Exception("Missing link to send customer info to"))

        val requestBody: RequestBody =
            gson.toJson(datatransTokenizationRequest).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(customerInfoPostUrl)
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request)
    }

    //TBI: write a generic function
    private suspend fun OkHttpClient.post(request: Request) =
        suspendCoroutine<Result<DatatransTokenizationResponseDto>> {
            newCall(request).enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    when {
                        response.isSuccessful -> {
                            val body = response.body?.string()
                            val tokenizationResponse: DatatransTokenizationResponseDto? = try {
                                gson.fromJson(body, DatatransTokenizationResponseDto::class.java)
                            } catch (e: JsonSyntaxException) {
                                Log.e("Datatrans", "Error parsing pre-registration response", e)
                                null
                            }

                            val result = if (tokenizationResponse == null) {
                                Result.failure(Exception("Missing content"))
                            } else {
                                Result.success(tokenizationResponse)
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
