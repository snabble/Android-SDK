package io.snabble.sdk.ui.payment.creditcard.datatrans.data

import com.google.gson.Gson
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.AuthDataDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.CustomerDataDto
import io.snabble.sdk.ui.payment.creditcard.shared.getTokenizationUrlFor
import io.snabble.sdk.ui.payment.creditcard.shared.post
import io.snabble.sdk.utils.GsonHolder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal interface DatatransRemoteDataSource {

    suspend fun sendUserData(customerDataDto: CustomerDataDto, projectId: String): Result<AuthDataDto>
}

internal class DatatransRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : DatatransRemoteDataSource {

    override suspend fun sendUserData(
        customerDataDto: CustomerDataDto,
        projectId: String
    ): Result<AuthDataDto> {
        val project = snabble.projects.find { it.id == projectId }
            ?: return Result.failure(Exception("Missing projectId"))

        val customerInfoPostUrl =
            project.paymentMethodDescriptors.getTokenizationUrlFor(customerDataDto.paymentMethod)
                ?: return Result.failure(Exception("Missing link to send customer info to"))

        val requestBody: RequestBody =
            gson.toJson(customerDataDto).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(customerInfoPostUrl)
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request, gson)
    }
}
