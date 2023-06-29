package io.snabble.sdk.events.data.payload

import io.snabble.sdk.events.data.EventType

internal data class PayloadError(
    val message: String? = null,
    val session: String? = null,
    override val eventType: EventType = EventType.ERROR
) : Payload
