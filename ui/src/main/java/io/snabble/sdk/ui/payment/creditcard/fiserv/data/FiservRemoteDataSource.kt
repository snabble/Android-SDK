package io.snabble.sdk.ui.payment.creditcard.fiserv.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.CustomerInfoDto
import io.snabble.sdk.ui.payment.creditcard.shared.data.ProviderRemoteDataSourceImpl
import io.snabble.sdk.ui.payment.creditcard.shared.getTokenizationUrlFor
import io.snabble.sdk.utils.GsonHolder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal interface FiservRemoteDataSource {

    suspend fun sendUserData(customerInfo: CustomerInfoDto, paymentMethod: PaymentMethod): Result<CreditCardAuthData>
}

internal class FiservRemoteDataSourceImpl(
    private val snabble: Snabble = Snabble,
    private val gson: Gson = GsonHolder.get(),
) : ProviderRemoteDataSourceImpl<CreditCardAuthData>(gson),
    FiservRemoteDataSource {

    override suspend fun sendUserData(
        customerInfo: CustomerInfoDto,
        paymentMethod: PaymentMethod
    ): Result<CreditCardAuthData> {
        val project = snabble.checkedInProject.value ?: return Result.failure(Exception("Missing projectId"))

        val customerInfoPostUrl = project.paymentMethodDescriptors.getTokenizationUrlFor(paymentMethod)
            ?: return Result.failure(Exception("Missing link to send customer info to"))

        val requestBody: RequestBody = gson.toJson(customerInfo).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(customerInfoPostUrl)
            .post(requestBody)
            .build()

        return project.okHttpClient.post(request)
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
