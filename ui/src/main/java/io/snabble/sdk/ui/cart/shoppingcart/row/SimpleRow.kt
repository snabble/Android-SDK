package io.snabble.sdk.ui.cart.shoppingcart.row

import androidx.annotation.DrawableRes

class SimpleRow : Row() {

    var text: String? = null
    var title: String? = null

    @DrawableRes
    var imageResId: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false

        val simpleRow = other as SimpleRow

        if (imageResId != simpleRow.imageResId) return false
        if (if (item != null) item != simpleRow.item else simpleRow.item != null) return false
        if (if (text != null) text != simpleRow.text else simpleRow.text != null) return false
        return if (title != null) title == simpleRow.title else simpleRow.title == null
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (if (item != null) item.hashCode() else 0)
        result = 31 * result + (if (text != null) text.hashCode() else 0)
        result = 31 * result + (if (title != null) title.hashCode() else 0)
        result = 31 * result + imageResId
        return result
    }
}
