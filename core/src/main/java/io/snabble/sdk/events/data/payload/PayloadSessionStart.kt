package io.snabble.sdk.events.data.payload

import io.snabble.sdk.events.data.EventType

internal data class PayloadSessionStart(
    val session: String? = null,
    override val eventType: EventType = EventType.SESSION_START
) : Payload
