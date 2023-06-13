package io.snabble.sdk.events.data.payload

import androidx.annotation.RestrictTo
import io.snabble.sdk.events.data.EventType

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface Payload {

    val eventType: EventType
}
