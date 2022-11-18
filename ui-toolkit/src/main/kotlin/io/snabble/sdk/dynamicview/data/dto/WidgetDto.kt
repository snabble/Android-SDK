package io.snabble.sdk.dynamicview.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WidgetDto {

    val id: String
}
