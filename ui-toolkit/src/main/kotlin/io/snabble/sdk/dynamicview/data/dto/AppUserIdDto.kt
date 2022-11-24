package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.AppUserIdItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.appUserId")
internal data class AppUserIdDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun AppUserIdDto.toAppUserId(): AppUserIdItem = AppUserIdItem(
    id = id,
    padding = padding.toPadding()
)
