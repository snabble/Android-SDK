package io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import java.util.Locale

internal data class DatatransTokenizationRequestDto(
    @SerializedName("paymentMethod") val paymentMethod: PaymentMethod,
    @SerializedName("language") val language: String = Locale.getDefault().language,
    @SerializedName("cardOwner") val cardOwner: CustomerInfoDto
)
