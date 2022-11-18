package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.locationPermission")
internal data class LocationPermissionDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun LocationPermissionDto.toLocationPermission(): LocationPermissionItem = LocationPermissionItem(
    id = id,
    padding = padding.toPadding()
)
