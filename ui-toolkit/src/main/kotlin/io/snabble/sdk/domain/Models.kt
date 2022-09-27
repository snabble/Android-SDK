package io.snabble.sdk.domain

data class Root(
    val configuration: Configuration,
    val widgets: List<Widget>,
)

data class Configuration(
    val image: Int?,
    val style: String,
    val padding: Padding
)

sealed interface Widget {

    val id: String
    val padding: Padding
}

interface HasText {
    val text: String
}

data class ButtonItem(
    override val id: String,
    override val text: String,
    val foregroundColorSource: Int?,
    val backgroundColorSource: Int?,
    override val padding: Padding,
) : Widget, HasText

data class CustomerCardItem(
    override val id: String,
    override val text: String,
    val imageSource: Int?,
    override val padding: Padding,
) : Widget, HasText

data class ConnectWifiItem(
    override val id: String,
    override val padding: Padding
) : Widget

data class ImageItem(
    override val id: String,
    val imageSource: Int?,
    override val padding: Padding,
) : Widget

data class InformationItem(
    override val id: String,
    override val text: String,
    val imageSource: Int?,
    override val padding: Padding,
) : Widget, HasText

data class LocationPermissionItem(
    override val id: String,
    override val padding: Padding,
) : Widget

data class PurchasesItem(
    override val id: String,
    val projectId: ProjectId,
    override val padding: Padding,
) : Widget

data class SectionItem(
    override val id: String,
    val header: String,
    val items: List<Widget>,
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

data class TextItem(
    override val id: String,
    override val text: String,
    val textColorSource: Int? = null,
    val textStyleSource: String? = null,
    val showDisclosure: Boolean,
    override val padding: Padding,
) : Widget, HasText

data class ToggleItem(
    override val id: String,
    override val text: String,
    val key: String,
    override val padding: Padding,
) : Widget, HasText

data class Padding(
    val start: Int = 0,
    val top: Int = 0,
    val end: Int = 0,
    val bottom: Int = 0,
) {
    constructor(all: Int)
        : this(start = all, top = all, end = all, bottom = all)

    constructor(horizontal: Int = 0, vertical: Int = 0)
        : this(start = horizontal, top = vertical, end = horizontal, bottom = vertical)
}

@JvmInline
value class ProjectId(val id: String)
