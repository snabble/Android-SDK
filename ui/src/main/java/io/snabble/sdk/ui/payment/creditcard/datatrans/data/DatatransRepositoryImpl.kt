package io.snabble.sdk.ui.payment.creditcard.datatrans.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.AddressDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.CustomerInfoDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.CustomerDataDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.AuthDataDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto.PhoneNumberDto
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.DatatransRepository
import io.snabble.sdk.ui.payment.creditcard.datatrans.domain.model.AuthData
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo
import java.util.Locale

internal class DatatransRepositoryImpl(
    private val datatransRemoteDataSource: DatatransRemoteDataSource = DatatransRemoteDataSourceImpl(),
) : DatatransRepository {

    override suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<AuthData> {
        return datatransRemoteDataSource.sendUserData(createTokenizationRequest(customerInfo, paymentMethod))
            .map { it.toResponse() }
    }
}

private fun createTokenizationRequest(
    customerInfo: CustomerInfo,
    paymentMethod: PaymentMethod
) = CustomerDataDto(
    paymentMethod = paymentMethod,
    language = Locale.getDefault().language,
    cardOwner = customerInfo.toDto()
)

private fun CustomerInfo.toDto() = CustomerInfoDto(
    name = name,
    email = email,
    address = AddressDto(
        street = address.street,
        city = address.city,
        country = address.country.numericCode,
        state = address.state,
        zip = address.zip
    ),
    phoneNumber = PhoneNumberDto(
        intCallingCode = intCallingCode,
        number = phoneNumber
    )
)

private fun AuthDataDto.toResponse() =
    AuthData(mobileToken = mobileToken, isTesting = isTesting)
