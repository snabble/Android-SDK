package io.snabble.sdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RootDto(
    @SerialName("configuration") val configuration: ConfigurationDto,
    @SerialName("widgets") val widgets: List<Widget>,
)

@Serializable
data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: Int,
)

@Serializable
sealed interface Widget {

    val id: String
}

@Serializable
@SerialName("spacer")
data class SpacerDto(
    @SerialName("id") override val id: String,
    @SerialName("length") val length: Int,
) : Widget

@Serializable
@SerialName("text")
data class TextDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("textColorSource") val textColorSource: String? = null,
    @SerialName("textStyleSource") val textStyleSource: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
) : Widget

@Serializable
@SerialName("image")
data class ImageDto(
    @SerialName("id") override val id: String,
    @SerialName("imageSource") val imageSource: String,
) : Widget

@SerialName("button")
data class ButtonDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("foregroundColorSource") val foregroundColorSource: String?,
    @SerialName("backgroundColorSource") val backgroundColorSource: String?,
) : Widget

@SerialName("information")
data class InformationDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("imageSource") val imageSource: String?,
    @SerialName("hideable") val hideable: Boolean?,
) : Widget

@SerialName("purchases")
data class PurchasesDto(
    @SerialName("id") override val id: String,
    @SerialName("projectId") val projectId: String,
) : Widget

@SerialName("toggle")
data class ToggleDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("key") val key: String,
) : Widget

@SerialName("section")
data class SectionDto(
    @SerialName("id") override val id: String,
    @SerialName("header") val header: String,
    @SerialName("items") val items: List<Widget>,
) : Widget

@SerialName("locationPermission")
data class LocationPermissionDto(
    @SerialName("id") override val id: String,
) : Widget
