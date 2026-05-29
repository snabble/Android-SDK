package io.snabble.sdk.shoppingcart.data.cart

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartCustomer(
    @SerializedName("loyaltyCard") val loyaltyCard: String
)
