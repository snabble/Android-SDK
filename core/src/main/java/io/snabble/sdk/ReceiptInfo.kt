package io.snabble.sdk

/**
 * Class containing information about a order
 */
data class ReceiptInfo(
    /**
     * Unique id of the receipt / order
     */
    @JvmField
    val id: String,
    /**
     * Get the project id of the order
     */
    @JvmField
    val projectId: String,
    /**
     * Unix timestamp of the purchase
     */
    @JvmField
    val timestamp: Long,
    /**
     * Url to the pdf document, containing the receipt
     */
    @JvmField
    val pdfUrl: String?,
    /**
     * Name of the shop in which the order was fulfilled
     */
    @JvmField
    val shopName: String,
    /**
     * Final price of the order
     */
    @JvmField
    val price: String,

    /**
     * State of the order
     */
    @JvmField
    val isSuccessful: Boolean
)
