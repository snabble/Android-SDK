package io.snabble.sdk.data

import io.snabble.sdk.data.serializers.PaddingValueListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RootDto(
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

@Serializable
data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: PaddingDto,
)

@Serializable
sealed interface WidgetDto {

    val id: String
    val padding: PaddingDto
}

@Serializable
@SerialName("button")
data class ButtonDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("foregroundColorSource") val foregroundColorSource: String? = null,
    @SerialName("backgroundColorSource") val backgroundColorSource: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("image")
data class ImageDto(
    @SerialName("id") override val id: String,
    @SerialName("imageSource") val imageSource: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("information")
data class InformationDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("imageSource") val imageSource: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("snabble.customerCard")
data class CustomerCardDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("imageSource") val imageSource: String? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("snabble.locationPermission")
data class LocationPermissionDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("purchases")
data class PurchasesDto(
    @SerialName("id") override val id: String,
    @SerialName("projectId") val projectId: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("section")
data class SectionDto(
    @SerialName("id") override val id: String,
    @SerialName("header") val header: String,
    @SerialName("items") val widgets: List<WidgetDto>,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("snabble.allStores")
data class SeeAllStoresDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("snabble.startShopping")
data class StartShoppingDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("snabble.connectWifi")
data class ConnectWifiDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("text")
data class TextDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("textColor") val textColorSource: String? = null,
    @SerialName("textStyle") val textStyleSource: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto

@Serializable
@SerialName("toggle")
data class ToggleDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("key") val key: String,
    @SerialName("padding") override val padding: PaddingDto,
) : WidgetDto
