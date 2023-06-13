package io.snabble.sdk.events.data.payload

import io.snabble.sdk.events.data.EventType

internal data class PayloadSessionEnd(
    val session: String? = null,
    override val eventType: EventType = EventType.SESSION_END
) : Payload
