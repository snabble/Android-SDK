package io.snabble.sdk;

public interface OnProductsAvailableListener {
    void onProductsAvailable(Product[] product, boolean wasOnline);

    void onProductsNotFound();

    void onError();
}
