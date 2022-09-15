package io.snabble.sdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RootDto(
    @SerialName("configuration") val configuration: ConfigurationDto,
    @SerialName("widgets") val widgets: List<Widget>,
)

data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: Int,
)

interface HasText {
    @SerialName("spacing")
    val text: String
}

@Serializable
sealed interface Widget {
    @SerialName("id")
    val id: Int

    @SerialName("spacing")
    val spacing: Int?
}

@Serializable
@SerialName("text")
data class TextDto(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("textColorSource") val textColorSource: String? = null,
    @SerialName("textStyleSource") val textStyleSource: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("spacing") override val spacing: Int,
) : Widget, HasText

@Serializable
@SerialName("image")
data class ImageDto(
    @SerialName("id") override val id: Int,
    @SerialName("imageSource") val imageSource: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("button")
data class ButtonDto(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("foregroundColorSource") val foregroundColorSource: String?,
    @SerialName("backgroundColorSource") val backgroundColorSource: String?,
    @SerialName("spacing") override val spacing: Int,
) : Widget, HasText

@SerialName("information")
data class InformationDto(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("imageSource") val imageSource: String?,
    @SerialName("hideable") val hideable: Boolean?,
    @SerialName("spacing") override val spacing: Int,
) : Widget, HasText

@SerialName("purchases")
data class PurchasesDto(
    @SerialName("id") override val id: Int,
    @SerialName("projectId") val projectId: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("toggle")
data class ToggleDto(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("key") val key: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget, HasText

@SerialName("section")
data class SectionDto(
    @SerialName("id") override val id: Int,
    @SerialName("header") val header: String,
    @SerialName("items") val items: List<Widget>,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("locationPermission")
data class LocationPermissionDto(
    @SerialName("id") override val id: Int,
    @SerialName("spacing") override val spacing: Int,
) : Widget
