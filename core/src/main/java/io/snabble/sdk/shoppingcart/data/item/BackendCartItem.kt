package io.snabble.sdk.shoppingcart.data.item

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartItem(
    val id: String? = null,
    val sku: String? = null,
    @JvmField @SerializedName("ItemID") val itemId: String? = null,
    val type: BackendCartItemType? = null,
    @JvmField val scannedCode: String? = null,
    @JvmField val amount: Int = 0,
    @JvmField val weightUnit: String? = null,
    @JvmField val price: Int? = null,
    @JvmField val weight: Int? = null,
    @JvmField val units: Int? = null,
    val refersTo: String? = null,
    val couponID: String? = null
)

enum class BackendCartItemType {
    @SerializedName("depositReturnVoucher")
    DEPOSIT_RETURN_VOUCHER,
}
