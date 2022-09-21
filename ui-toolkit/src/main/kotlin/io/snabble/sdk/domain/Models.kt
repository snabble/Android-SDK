package io.snabble.sdk.domain

data class Root(
    val configuration: Configuration,
    val widgets: List<Widget>,
)

data class Configuration(
    val image: Int?,
    val style: String
)

sealed interface Widget {

    val id: String
    val padding: Padding
}

interface HasText {
    val text: String
}

data class TextItem(
    override val id: String,
    override val text: String,
    val textColorSource: Int? = null,
    val textStyleSource: String? = null,
    val showDisclosure: Boolean,
    override val padding: Padding,
) : Widget, HasText

data class ImageItem(
    override val id: String,
    val imageSource: Int?,
    override val padding: Padding,
) : Widget

data class ButtonItem(
    override val id: String,
    override val text: String,
    val foregroundColorSource: Int?,
    val backgroundColorSource: Int?,
    override val padding: Padding,
) : Widget, HasText

data class InformationItemItem(
    override val id: String,
    override val text: String,
    val imageSource: Int?,
    val hideable: Boolean,
    override val padding: Padding,
) : Widget, HasText

data class PurchasesItem(
    override val id: String,
    val projectId: String,
    override val padding: Padding,
) : Widget

data class ToggleItem(
    override val id: String,
    override val text: String,
    val key: String,
    override val padding: Padding,
) : Widget, HasText

data class SectionItem(
    override val id: String,
    val header: String,
    val items: List<Widget>,
    override val padding: Padding,
) : Widget

data class LocationPermissionItem(
    override val id: String,
    override val padding: Padding,
) : Widget

data class SeeAllStoresItem(
    override val id: String,
    override val padding: Padding
) : Widget

data class StartShoppingItem(
    override val id: String,
    override val padding: Padding
) : Widget

data class Padding(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
) {

    constructor(all: Int)
        : this(start = all, top = all, end = all, bottom = all)

    constructor(horizontal: Int, vertical: Int)
        : this(start = horizontal, top = vertical, end = horizontal, bottom = vertical)
}

