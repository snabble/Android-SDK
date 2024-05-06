package io.snabble.sdk.shoppingcart.data.cart

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartRequiredInformation(
    val id: String? = null,
    val value: String? = null
)
