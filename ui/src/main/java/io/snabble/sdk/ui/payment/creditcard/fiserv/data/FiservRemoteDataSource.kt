package io.snabble.sdk.ui.payment.creditcard.fiserv.data

import com.google.gson.Gson
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.AuthDataDto
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.CustomerInfoDto
import io.snabble.sdk.ui.payment.creditcard.shared.getTokenizationUrlFor
import io.snabble.sdk.ui.payment.creditcard.shared.post
import io.snabble.sdk.utils.GsonHolder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal interface FiservRemoteDataSource {

    suspend fun sendUserData(
        customerInfo: CustomerInfoDto,
        paymentMethod: PaymentMethod
    ): Result<AuthDataDto>
}

internal class FiservRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : FiservRemoteDataSource {

    override suspend fun sendUserData(
        customerInfo: CustomerInfoDto,
        paymentMethod: PaymentMethod
    ): Result<AuthDataDto> {
        val project =
            snabble.checkedInProject.value ?: return Result.failure(Exception("Missing projectId"))

        val customerInfoPostUrl =
            project.paymentMethodDescriptors.getTokenizationUrlFor(paymentMethod)
                ?: return Result.failure(Exception("Missing link to send customer info to"))

        val requestBody: RequestBody =
            gson.toJson(customerInfo).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(customerInfoPostUrl)
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request, gson)
    }
}
