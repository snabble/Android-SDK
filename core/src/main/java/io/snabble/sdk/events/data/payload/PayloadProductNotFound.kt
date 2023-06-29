package io.snabble.sdk.events.data.payload

import io.snabble.sdk.events.data.EventType

internal data class PayloadProductNotFound(
    val scannedCode: String? = null,
    val matched: Map<String?, String?>? = null,
    override val eventType: EventType = EventType.PRODUCT_NOT_FOUND
) : Payload
