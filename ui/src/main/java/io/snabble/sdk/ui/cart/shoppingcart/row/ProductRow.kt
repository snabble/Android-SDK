package io.snabble.sdk.ui.cart.shoppingcart.row

import io.snabble.sdk.Unit

class ProductRow : Row() {

    var name: String? = null
    var subtitle: String? = null
    var imageUrl: String? = null
    var encodingUnit: Unit? = null
    var priceText: String? = null
    var quantityText: String? = null
    var quantity: Int = 0
    var editable: Boolean = false
    var manualDiscountApplied: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false

        val that = other as ProductRow

        if (quantity != that.quantity) return false
        if (editable != that.editable) return false
        if (manualDiscountApplied != that.manualDiscountApplied) return false
        if (if (name != null) name != that.name else that.name != null) return false
        if (if (subtitle != null) subtitle != that.subtitle else that.subtitle != null) return false
        if (if (imageUrl != null) imageUrl != that.imageUrl else that.imageUrl != null) return false
        if (encodingUnit != that.encodingUnit) return false
        if (if (priceText != null) priceText != that.priceText else that.priceText != null) return false
        return if (quantityText != null) quantityText == that.quantityText else that.quantityText == null
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (if (name != null) name.hashCode() else 0)
        result = 31 * result + (if (subtitle != null) subtitle.hashCode() else 0)
        result = 31 * result + (if (imageUrl != null) imageUrl.hashCode() else 0)
        result = 31 * result + (if (encodingUnit != null) encodingUnit.hashCode() else 0)
        result = 31 * result + (if (priceText != null) priceText.hashCode() else 0)
        result = 31 * result + (if (quantityText != null) quantityText.hashCode() else 0)
        result = 31 * result + quantity
        result = 31 * result + (if (editable) 1 else 0)
        result = 31 * result + (if (manualDiscountApplied) 1 else 0)
        return result
    }
}
