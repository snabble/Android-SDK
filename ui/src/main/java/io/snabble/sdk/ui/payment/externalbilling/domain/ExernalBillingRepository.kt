package io.snabble.sdk.ui.payment.externalbilling.domain

import android.util.Log
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.externalbilling.data.BillingCredentials
import io.snabble.sdk.ui.payment.externalbilling.data.BillingCredentialsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface ExternalBillingRepository {

    suspend fun login(username: String, password: String): Result<BillingCredentialsResponse>

    suspend fun addPaymentCredentials()
}

class ExternalBillingRepositoryImpl(
    private val project: Project,
    private val json: Json
) : ExternalBillingRepository {

    override suspend fun login(username: String, password: String): Result<BillingCredentialsResponse> =
        withContext(Dispatchers.IO) request@{
            val response = loginService(username, password)
            val body = response?.body
            return@request if (response != null && response.isSuccessful && body != null) {
                val jsonData = body.string()
                val responseCredentials = json.decodeFromString<BillingCredentialsResponse>(jsonData)
                Result.success(responseCredentials)
            } else {
                Result.failure(Exception(response?.message))
            }
        }

    private suspend fun loginService(
        username: String,
        password: String
    ): Response? {
        val okHttpClient = project.okHttpClient
        val json = Json { ignoreUnknownKeys = true }
        return try {
            okHttpClient
                .newCall(
                    Request.Builder()
                        .url("${Snabble.endpointBaseUrl}/${project.id}/external-billing/credentials/auth")
                        .post(
                            json
                                .encodeToString(
                                    BillingCredentials(username, password)
                                )
                                .toRequestBody("application/json".toMediaType())
                        )
                        .build()
                )
                .await()
        } catch (e: IOException) {
            Log.e("xx", "login for external billing failed: ${e.message} ")
            null
        }
    }

    override suspend fun addPaymentCredentials() {
        TODO("Not yet implemented")
    }
}

private suspend inline fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }
    })

    continuation.invokeOnCancellation {
        if (continuation.isCancelled) {
            cancel()
        }
    }
}


