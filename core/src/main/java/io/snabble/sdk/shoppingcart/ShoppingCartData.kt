package io.snabble.sdk.shoppingcart

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.Product
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.utils.GsonHolder
import java.util.UUID

@Keep
data class ShoppingCartData @JvmOverloads constructor(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("uuid") val uuid: String = UUID.randomUUID().toString(),
    @SerializedName("lastModificationTime") val lastModificationTime: Long = 0,
    @SerializedName("items") val items: MutableList<ShoppingCart.Item> = mutableListOf(),
    @SerializedName("violationNotifications")
    val violationNotifications: MutableList<ViolationNotification> = mutableListOf(),
    @SerializedName("modCount") val modCount: Int = 0,
    @SerializedName("addCount") val addCount: Int = 0,
    @SerializedName("onlineTotalPrice") val onlineTotalPrice: Int? = null,
    @SerializedName("invalidProducts") val invalidProducts: List<Product>? = null,
    @SerializedName("invalidItemIds") val invalidItemIds: List<String>? = null,
    @SerializedName("taxation") val taxation: Taxation = Taxation.UNDECIDED,
    @SerializedName("hasReachedMaxCheckoutLimit") val hasReachedMaxCheckoutLimit: Boolean = false,
    @SerializedName("hasReachedMaxOnlinePaymentLimit") val hasReachedMaxOnlinePaymentLimit: Boolean = false,
    @SerializedName("backupTimestamp") val backupTimestamp: Long = 0,
) {

    fun applyShoppingCart(shoppingCart: ShoppingCart) {
        items.forEach {
            it.cart = shoppingCart
        }
    }

    fun deepCopy(): ShoppingCartData {
        val json = GsonHolder.get().toJson(this)
        return GsonHolder.get().fromJson(json, ShoppingCartData::class.java)
    }
}
