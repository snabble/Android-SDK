package io.snabble.sdk.ui.cart;

import io.snabble.sdk.shoppingcart.ShoppingCart;

public interface UndoHelper {
    void removeAndShowUndoSnackbar(final int adapterPosition, final ShoppingCart.Item item);
}
