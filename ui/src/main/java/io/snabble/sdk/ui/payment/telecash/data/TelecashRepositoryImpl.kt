package io.snabble.sdk.ui.payment.telecash.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.domain.TelecashRepository
import io.snabble.sdk.ui.payment.telecash.domain.UserDetails

class TelecashRepositoryImpl(
    private val remoteDataSource: TelecashRemoteDataSource
) : TelecashRepository {

    override suspend fun preAuth(userDetails: UserDetails, paymentMethod: PaymentMethod): Result<PreAuthInformation?> =
        remoteDataSource.preAuth(userDetails.toDto(), paymentMethod)
            .map { PreAuthInformation(it.links.formUrl.href, it.links.deleteUrl.href) }
}

private fun UserDetails.toDto() = UserDetailsDto(
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

data class PreAuthInformation(
    val formUrl: String,
    val deleteUrl: String
)
