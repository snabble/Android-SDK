package io.snabble.sdk

/**
 * Class containing information about a order
 */
data class ReceiptInfo(
    /**
     * Unique id of the receipt / order
     */
    val id: String,
    /**
     * Get the project id of the order
     */
    val projectId: String,
    /**
     * Unix timestamp of the purchase
     */
    val timestamp: Long,
    /**
     * Url to the pdf document, containing the receipt
     */
    val pdfUrl: String,
    /**
     * Name of the shop in which the order was fulfilled
     */
    val shopName: String,
    /**
     * Final price of the order
     */
    val price: String
)