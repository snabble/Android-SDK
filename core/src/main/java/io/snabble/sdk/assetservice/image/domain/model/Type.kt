package io.snabble.sdk.assetservice.image.domain.model

/**
 * Enum class for describing the image type
 */
enum class Type(val value: String) {

    SVG(".svg"),
    JPG(".jpg"),
    WEBP(".webp")
}
