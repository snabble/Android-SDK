package io.snabble.sdk.domain

data class Root(
    val configuration: Configuration,
    val widgets: List<Widget>,
)

data class Configuration(
    val image: Int?,
    val style: String,
    val padding: Int,
)

sealed interface Widget {
    val id: Int
    val spacing: Int?
    val padding: Int
}

interface HasText {
    val text: String
}

data class Text(
    override val id: Int,
    override val text: String,
    val textColorSource: Int? = null,
    val textStyleSource: String? = null,
    val showDisclosure: Boolean,
    override val spacing: Int,
    override val padding: Int
) : Widget, HasText

data class Image(
    override val id: Int,
    val imageSource: Int?,
    override val spacing: Int,
    override val padding: Int
) : Widget

data class Button(
    override val id: Int,
    override val text: String,
    val foregroundColorSource: Int?,
    val backgroundColorSource: Int?,
    override val spacing: Int,
    override val padding: Int
) : Widget, HasText

data class Information(
    override val id: Int,
    override val text: String,
    val imageSource: Int?,
    val hideable: Boolean,
    override val spacing: Int,
    override val padding: Int
) : Widget, HasText

data class Purchases(
    override val id: Int,
    val projectId: String,
    override val spacing: Int,
    override val padding: Int
) : Widget

data class Toggle(
    override val id: Int,
    override val text: String,
    val key: String,
    override val spacing: Int,
    override val padding: Int
) : Widget, HasText

data class Section(
    override val id: Int,
    val header: String,
    val items: List<Widget>,
    override val spacing: Int,
    override val padding: Int
) : Widget

data class LocationPermission(
    override val id: Int,
    override val spacing: Int,
    override val padding: Int
) : Widget
