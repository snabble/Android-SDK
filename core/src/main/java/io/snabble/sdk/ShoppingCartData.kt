package io.snabble.sdk

import io.snabble.sdk.utils.GsonHolder
import java.util.*

data class ShoppingCartData @JvmOverloads constructor(
    @JvmField var id: String = UUID.randomUUID().toString(),
    @JvmField var uuid: String? = UUID.randomUUID().toString(),
    @JvmField var lastModificationTime: Long = 0,
    @JvmSuppressWildcards @JvmField var items: MutableList<ShoppingCart.Item> = mutableListOf(),
    @JvmField var violationNotifications: List<ViolationNotification> = emptyList(),
    @JvmField var modCount: Int = 0,
    @JvmField var addCount: Int = 0,
    @JvmField var onlineTotalPrice: Int? = null,
    @JvmSuppressWildcards @JvmField var invalidProducts: List<Product>? = null,
    @JvmField var taxation: ShoppingCart.Taxation = ShoppingCart.Taxation.UNDECIDED,
    @JvmField var hasRaisedMaxCheckoutLimit: Boolean = false,
    @JvmField var hasRaisedMaxOnlinePaymentLimit: Boolean = false,
    @JvmField var invalidDepositReturnVoucher: Boolean = false,
    @JvmField var backupTimestamp: Long = 0,
) {
    fun applyShoppingCart(shoppingCart: ShoppingCart) {
        items.forEach {
            it.cart = shoppingCart
        }
    }

    fun deepCopy() : ShoppingCartData {
        val json = GsonHolder.get().toJson(this)
        return GsonHolder.get().fromJson<ShoppingCartData>(json, ShoppingCartData::class.java)
    }
}