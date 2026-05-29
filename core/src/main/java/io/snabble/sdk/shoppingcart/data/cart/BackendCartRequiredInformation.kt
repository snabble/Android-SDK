package io.snabble.sdk.shoppingcart.data.cart

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartRequiredInformation(
    @SerializedName("id") val id: String,
    @SerializedName("value") val value: String
)
