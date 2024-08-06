package io.snabble.sdk.ui.payment.telecash.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.domain.TelecashRepository
import io.snabble.sdk.ui.payment.telecash.domain.CustomerInfo

internal class TelecashRepositoryImpl(
    private val remoteDataSource: TelecashRemoteDataSource = TelecashRemoteDataSourceImpl()
) : TelecashRepository {

    override suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<CreditCardAdditionInfo> =
        remoteDataSource
            .sendUserData(customerInfo.toDto(), paymentMethod)
            .map { CreditCardAdditionInfo(it.links.formUrl.href, it.links.deleteUrl.href) }
}

private fun CustomerInfo.toDto() = CustomerInfoDto(
    name = name,
    phoneNumber = phoneNumber,
    email = email,
    address = AddressDto(
        street = address.street,
        zip = address.zip,
        city = address.city,
        state = address.state,
        country = address.country
    ),
)

data class CreditCardAdditionInfo(
    val formUrl: String,
    val preAuthDeleteUrl: String
)