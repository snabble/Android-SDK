package io.snabble.sdk.shoppingcart.data.item

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCartItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @JvmField @SerializedName("ItemID") val itemId: String? = null,
    @SerializedName("type") val type: BackendCartItemType? = null,
    @JvmField @SerializedName("scannedCode") val scannedCode: String? = null,
    @JvmField @SerializedName("amount") val amount: Int = 0,
    @JvmField @SerializedName("weightUnit") val weightUnit: String? = null,
    @JvmField @SerializedName("price") val price: Int? = null,
    @JvmField @SerializedName("weight") val weight: Int? = null,
    @JvmField @SerializedName("units") val units: Int? = null,
    @SerializedName("refersTo") val refersTo: String? = null,
    @SerializedName("couponID") val couponID: String? = null
)

enum class BackendCartItemType {
    @SerializedName("depositReturnVoucher")
    DEPOSIT_RETURN_VOUCHER,
}
