package io.snabble.sdk.ui.payment.fiserv.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.fiserv.domain.CustomerInfo
import io.snabble.sdk.ui.payment.fiserv.domain.FiservRepository

internal class FiservRepositoryImpl(
    private val remoteDataSource: FiservRemoteDataSource = FiservRemoteDataSourceImpl()
) : FiservRepository {

    override suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<FiservCardRegisterUrls> =
        remoteDataSource
            .sendUserData(customerInfo.toDto(), paymentMethod)
            .map { FiservCardRegisterUrls(it.links.formUrl.href, it.links.deleteUrl.href) }
}

private fun CustomerInfo.toDto() = CustomerInfoDto(
    name = name,
    phoneNumber = phoneNumber,
    email = email,
    address = AddressDto(
        street = address.street,
        zip = address.zip,
        city = address.city,
        state = address.state.ifEmpty { null },
        country = address.country
    ),
)

data class FiservCardRegisterUrls(
    val formUrl: String,
    val preAuthDeleteUrl: String
)
