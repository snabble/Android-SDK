package io.snabble.sdk

import io.snabble.sdk.Product

/**
 * Interface for product api calls
 */
interface OnProductAvailableListener {
    /**
     * Called when a product is found
     */
    fun onProductAvailable(product: Product, wasOnline: Boolean)

    /**
     * Call was successful but no product was found
     */
    fun onProductNotFound()

    /**
     * Call was not successful (connection error, database error, ...)
     */
    fun onError()
}