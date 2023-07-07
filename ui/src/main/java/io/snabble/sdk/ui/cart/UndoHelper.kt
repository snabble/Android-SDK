package io.snabble.sdk.ui.cart

import io.snabble.sdk.ShoppingCart

interface UndoHelper {

    fun removeAndShowUndoSnackbar(adapterPosition: Int, item: ShoppingCart.Item?)
}
