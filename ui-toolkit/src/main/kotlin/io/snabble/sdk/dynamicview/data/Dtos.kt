package io.snabble.sdk.dynamicview.data

import io.snabble.sdk.dynamicview.data.serializers.PaddingValueListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DynamicConfigDto(
    @SerialName("configuration") val configuration: ConfigurationDto,
    @SerialName("widgets") val widgets: List<WidgetDto>,
)

@Serializable(with = PaddingValueListSerializer::class)
@SerialName("padding")
data class PaddingDto(
    @SerialName("left") val start: Int,
    @SerialName("top") val top: Int,
    @SerialName("right") val end: Int,
    @SerialName("bottom") val bottom: Int,
)

interface HasPadding {

    val padding: PaddingDto
}

@Serializable
data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: PaddingDto,
)

@Serializable
sealed interface WidgetDto {

    val id: String
}

@Serializable
@SerialName("button")
data class ButtonDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("foregroundColor") val foregroundColor: String? = null,
    @SerialName("backgroundColor") val backgroundColor: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("image")
data class ImageDto(
    @SerialName("id") override val id: String,
    @SerialName("image") val image: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("information")
data class InformationDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.customerCard")
data class CustomerCardDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.connectWifi")
data class ConnectWlanDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.locationPermission")
data class LocationPermissionDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("purchases")
data class PurchasesDto(
    @SerialName("id") override val id: String,
    @SerialName("projectId") val projectId: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("section")
data class SectionDto(
    @SerialName("id") override val id: String,
    @SerialName("header") val header: String,
    @SerialName("items") val widgets: List<WidgetDto>,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.allStores")
data class SeeAllStoresDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("snabble.startShopping")
data class StartShoppingDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("text")
data class TextDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("textColor") val textColor: String? = null,
    @SerialName("textStyle") val textStyle: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding

@Serializable
@SerialName("toggle")
data class ToggleDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("key") val key: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto, HasPadding
