package io.snabble.sdk.dynamicview.data

import io.snabble.sdk.dynamicview.data.serializers.PaddingValueListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DynamicConfigDto(
    @SerialName("configuration") val configuration: ConfigurationDto,
    @SerialName("widgets") val widgets: List<WidgetDto>,
)

@Serializable(with = PaddingValueListSerializer::class)
@SerialName("padding")
internal data class PaddingDto(
    @SerialName("left") val start: Int,
    @SerialName("top") val top: Int,
    @SerialName("right") val end: Int,
    @SerialName("bottom") val bottom: Int,
)

internal interface HasPadding {

    val padding: PaddingDto
}

@Serializable
internal data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: PaddingDto,
)

@Serializable
internal sealed interface WidgetDto {

    val id: String
}

@Serializable
@SerialName("button")
internal data class ButtonDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("foregroundColor") val foregroundColor: String? = null,
    @SerialName("backgroundColor") val backgroundColor: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("image")
internal data class ImageDto(
    @SerialName("id") override val id: String,
    @SerialName("image") val image: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("information")
internal data class InformationDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.customerCard")
internal data class CustomerCardDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.connectWifi")
internal data class ConnectWlanDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.locationPermission")
internal data class LocationPermissionDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("purchases")
internal data class PurchasesDto(
    @SerialName("id") override val id: String,
    @SerialName("projectId") val projectId: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("section")
internal data class SectionDto(
    @SerialName("id") override val id: String,
    @SerialName("header") val header: String,
    @SerialName("items") val widgets: List<WidgetDto>,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.allStores")
internal data class SeeAllStoresDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.startShopping")
internal data class StartShoppingDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("text")
internal data class TextDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("textColor") val textColor: String? = null,
    @SerialName("textStyle") val textStyle: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("toggle")
internal data class ToggleDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("key") val key: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding
