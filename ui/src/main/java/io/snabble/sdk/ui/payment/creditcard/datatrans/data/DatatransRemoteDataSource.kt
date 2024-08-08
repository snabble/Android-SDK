package io.snabble.sdk.ui.payment.creditcard.datatrans.data

import com.google.gson.Gson
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.DatatransTokenizationRequestDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.DatatransTokenizationResponseDto
import io.snabble.sdk.ui.payment.creditcard.shared.data.ProviderRemoteDataSourceImpl
import io.snabble.sdk.ui.payment.creditcard.shared.getTokenizationUrlFor
import io.snabble.sdk.utils.GsonHolder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal interface DatatransRemoteDataSource {

    suspend fun sendUserData(
        datatransTokenizationRequest: DatatransTokenizationRequestDto,
    ): Result<DatatransTokenizationResponseDto>
}

internal class DatatransRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : ProviderRemoteDataSourceImpl<DatatransTokenizationResponseDto>(gson),
    DatatransRemoteDataSource {

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
}
