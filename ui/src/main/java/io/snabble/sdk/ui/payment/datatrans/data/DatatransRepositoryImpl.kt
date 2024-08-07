package io.snabble.sdk.ui.payment.datatrans.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.datatrans.data.dto.AddressDto
import io.snabble.sdk.ui.payment.datatrans.data.dto.CustomerInfoDto
import io.snabble.sdk.ui.payment.datatrans.data.dto.DatatransTokenizationRequestDto
import io.snabble.sdk.ui.payment.datatrans.data.dto.DatatransTokenizationResponseDto
import io.snabble.sdk.ui.payment.datatrans.data.dto.PhoneNumberDto
import io.snabble.sdk.ui.payment.datatrans.domain.DatatransRepository
import io.snabble.sdk.ui.payment.datatrans.domain.model.CustomerInfo
import io.snabble.sdk.ui.payment.datatrans.domain.model.DatatransTokenizationResponse
import java.util.Locale

internal class DatatransRepositoryImpl(
    private val datatransRemoteDataSource: DatatransRemoteDataSource = DatatransRemoteDataSourceImpl(),
) : DatatransRepository {

    override suspend fun sendUserData(
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod
    ): Result<DatatransTokenizationResponse> {
        return datatransRemoteDataSource.sendUserData(createTokenizationRequest(customerInfo, paymentMethod))
            .map { it.toResponse() }
    }
}

private fun createTokenizationRequest(
    customerInfo: CustomerInfo,
    paymentMethod: PaymentMethod
) = DatatransTokenizationRequestDto(
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
        country = address.country,
        state = address.state,
        zip = address.zip
    ),
    phoneNumber = PhoneNumberDto(
        intCallingCode = countryCode,
        number = subscriber
    )
)

private fun DatatransTokenizationResponseDto.toResponse() =
    DatatransTokenizationResponse(mobileToken = mobileToken, isTesting = isTesting)
