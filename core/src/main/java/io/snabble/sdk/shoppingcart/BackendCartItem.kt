package io.snabble.sdk.shoppingcart

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartItem(

    @JvmField val id: String? = null,
    @JvmField val sku: String? = null,
    @JvmField val scannedCode: String? = null,
    @JvmField val amount: Int = 0,
    @JvmField val weightUnit: String? = null,
    @JvmField val price: Int? = null,
    @JvmField val weight: Int? = null,
    @JvmField val units: Int? = null,
    @JvmField val refersTo: String? = null,
    @JvmField val couponID: String? = null,
)

