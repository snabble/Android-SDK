package io.snabble.sdk.ui.cart;

import io.snabble.sdk.ShoppingCart;

public interface UndoHelper {
    void removeAndShowUndoSnackbar(final int adapterPosition, final ShoppingCart.Item item);
}