package io.snabble.sdk

/**
 * Enum describing a barcode format
 */
enum class BarcodeFormat {
    CODE_39,
    CODE_128,
    EAN_8,
    EAN_13,
    ITF_14,
    QR_CODE,
    DATA_MATRIX,
    PDF_417;

    companion object {
        /**
         * Parse the BarcodeFormat from a string
         */
        fun parse(str: String?): BarcodeFormat? {
            return when (str) {
                "code39" -> CODE_39
                "code128" -> CODE_128
                "ean8" -> EAN_8
                "ean13" -> EAN_13
                "itf14" -> ITF_14
                "datamatrix" -> DATA_MATRIX
                "pdf417" -> PDF_417
                "qr", "qrCode" -> QR_CODE
                else -> null
            }
        }
    }
}