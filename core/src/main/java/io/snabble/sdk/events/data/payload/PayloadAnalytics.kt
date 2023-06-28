package io.snabble.sdk.events.data.payload

import io.snabble.sdk.events.data.EventType

internal data class PayloadAnalytics(
    val key: String? = null,
    val value: String? = null,
    val comment: String? = null,
    val session: String? = null,
    override val eventType: EventType = EventType.ANALYTICS
) : Payload
