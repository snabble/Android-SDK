package io.snabble.sdk.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Widget {
    val id: Int
}

@Serializable
@SerialName("text")
data class TextModel(
    override val id: Int,
    val text: String,
    val textColorSource: String? = null,
    val textStyleSource: String? = null,
    val showDisclosure: Boolean? = null,
    val spacing: Float? = null,
) : Widget

@Serializable
@SerialName("image")
data class ImageModel(
    override val id: Int,
    val imageSource: String,
    val spacing: Float,
) : Widget

@SerialName("button")
data class ButtonModel(
    override val id: Int,
    val text: String,
    val foregroundColorSource: String?,
    val backgroundColorSource: String?,
    val spacing: Float?
) : Widget

@SerialName("information")
data class InformationModel(
    override val id: Int,
    val text: String,
    val imageSource: String?,
    val hideable: Boolean?,
    val spacing: Float?
) : Widget

@SerialName("purchases")
data class PurchasesModel(
    override val id: Int,
    val projectId: String,
    val spacing: Float?
) : Widget

@SerialName("toggle")
data class ToggleModel(
    override val id: Int,
    val text: String,
    val key: String,
    val spacing: Float?
) : Widget

@SerialName("section")
data class SectionModel(
    override val id: Int,
    val header: String,
    val items: List<Widget>,
    val spacing: Float?
) : Widget

@SerialName("locationPermission")
data class LocationPermissionModel(
    override val id: Int,
    val spacing: Float?
) : Widget
