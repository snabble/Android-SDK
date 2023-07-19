package io.snabble.sdk.ui.cart.adapter.models

import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Unit
import io.snabble.sdk.ui.utils.emptyToNull

data class ProductRow(
    override val item: ShoppingCart.Item? = null,
    override val isDismissible: Boolean = false,
    val name: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val encodingUnit: Unit? = null,
    val priceText: String? = null,
    val quantityText: String? = null,
    val quantity: Int = 0,
    val editable: Boolean = false,
    val manualDiscountApplied: Boolean = false,
) : Row

internal fun ShoppingCart.Item.productRowFromProduct(): ProductRow = ProductRow(
    item = this,
    isDismissible = true,
    name = displayName.emptyToNull(),
    subtitle = product?.subtitle?.emptyToNull(),
    imageUrl = product?.imageUrl?.emptyToNull(),
    encodingUnit = unit,
    priceText = totalPriceText?.emptyToNull(),
    quantityText = quantityText?.emptyToNull(),
    quantity = quantity,
    editable = isEditable,
    manualDiscountApplied = isManualCouponApplied
)
