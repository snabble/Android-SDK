package io.snabble.sdk;

public interface OnProductAvailableListener {
    void onProductAvailable(Product product, boolean wasOnline);

    void onProductNotFound();

    void onError();
}
