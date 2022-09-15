package io.snabble.sdk.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Root(
    @SerialName("configuration") val configuration: Configuration,
    @SerialName("widgets") val widgets: List<Widget>,
)

data class Configuration(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: Int,
)

interface Text {
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
data class TextModel(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("textColorSource") val textColorSource: String? = null,
    @SerialName("textStyleSource") val textStyleSource: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("spacing") override val spacing: Int,
) : Widget, Text

@Serializable
@SerialName("image")
data class ImageModel(
    @SerialName("id") override val id: Int,
    @SerialName("imageSource") val imageSource: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("button")
data class ButtonModel(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("foregroundColorSource") val foregroundColorSource: String?,
    @SerialName("backgroundColorSource") val backgroundColorSource: String?,
    @SerialName("spacing") override val spacing: Int,
) : Widget, Text

@SerialName("information")
data class InformationModel(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("imageSource") val imageSource: String?,
    @SerialName("hideable") val hideable: Boolean?,
    @SerialName("spacing") override val spacing: Int,
) : Widget, Text

@SerialName("purchases")
data class PurchasesModel(
    @SerialName("id") override val id: Int,
    @SerialName("projectId") val projectId: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("toggle")
data class ToggleModel(
    @SerialName("id") override val id: Int,
    @SerialName("text") override val text: String,
    @SerialName("key") val key: String,
    @SerialName("spacing") override val spacing: Int,
) : Widget, Text

@SerialName("section")
data class SectionModel(
    @SerialName("id") override val id: Int,
    @SerialName("header") val header: String,
    @SerialName("items") val items: List<Widget>,
    @SerialName("spacing") override val spacing: Int,
) : Widget

@SerialName("locationPermission")
data class LocationPermissionModel(
    @SerialName("id") override val id: Int,
    @SerialName("spacing") override val spacing: Int,
) : Widget
