package io.snabble.sdk.ui.payment.creditcard.fiserv.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.AddressDto
import io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto.CustomerInfoDto
import io.snabble.sdk.ui.payment.creditcard.fiserv.domain.FiservRepository
import io.snabble.sdk.ui.payment.creditcard.fiserv.domain.model.AuthData
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo

internal class FiservRepositoryImpl(
    private val remoteDataSource: FiservRemoteDataSource = FiservRemoteDataSourceImpl()
) : FiservRepository {

    override suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod,
        projectId: String
    ): Result<AuthData> =
        remoteDataSource
            .sendUserData(customerInfo.toDto(), paymentMethod, projectId)
            .map { AuthData(it.links.formUrl.href, it.links.deleteUrl.href) }
}

private fun CustomerInfo.toDto() = CustomerInfoDto(
    name = name,
    phoneNumber = "$intCallingCode$phoneNumber",
    email = email,
    address = AddressDto(
        street = address.street,
        zip = address.zip,
        city = address.city,
        state = address.state.ifEmpty { null },
        country = address.country.code
    ),
)
