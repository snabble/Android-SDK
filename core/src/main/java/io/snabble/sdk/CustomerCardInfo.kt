package io.snabble.sdk

/**
 * Class describing customer cards
 */
data class CustomerCardInfo(
    /**
     * Unique id of the customer card type
     **/
    val cardId: String,
    /**
     * True if the customer card is required for checkout, false otherwise
     */
    val isRequired: Boolean
)