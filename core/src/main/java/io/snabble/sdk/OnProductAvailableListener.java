package io.snabble.sdk;

public interface OnProductAvailableListener {
    void onProductAvailable(Product product, boolean wasOnlineProduct);

    void onProductNotFound();

    void onError();
}
