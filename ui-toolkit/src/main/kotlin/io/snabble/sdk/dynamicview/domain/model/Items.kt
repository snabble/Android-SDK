package io.snabble.sdk.dynamicview.domain.model

data class DynamicConfig(
    val configuration: Configuration,
    val widgets: List<Widget>,
)

data class Configuration(
    val image: Int?,
    val style: String,
    val padding: Padding,
)

sealed interface Widget {

    val id: String
}

interface HasText {

    val text: String
}

data class ButtonItem(
    override val id: String,
    override val text: String,
    val foregroundColor: Int?,
    val backgroundColor: Int?,
    override val padding: Padding,
) : Widget, HasPadding, HasText

data class CustomerCardItem(
    override val id: String,
    override val text: String,
    val image: Int?,
    override val padding: Padding,
) : Widget, HasPadding, HasText

data class ConnectWlanItem(
    override val id: String,
    override val padding: Padding,
) : Widget, HasPadding

data class ImageItem(
    override val id: String,
    val image: Int?,
    override val padding: Padding,
) : Widget, HasPadding

data class InformationItem(
    override val id: String,
    override val text: String,
    val image: Int?,
    override val padding: Padding,
) : Widget, HasPadding, HasText

data class LocationPermissionItem(
    override val id: String,
    override val padding: Padding,
) : Widget, HasPadding

data class PurchasesItem(
    override val id: String,
    val projectId: ProjectId,
    override val padding: Padding,
) : Widget, HasPadding

data class SectionItem(
    override val id: String,
    val header: String,
    val items: List<Widget>,
    override val padding: Padding,
) : Widget, HasPadding

data class SeeAllStoresItem(
    override val id: String,
    override val padding: Padding,
) : Widget, HasPadding

data class StartShoppingItem(
    override val id: String,
    override val padding: Padding,
) : Widget, HasPadding

data class TextItem(
    override val id: String,
    override val text: String,
    val textColor: Int? = null,
    val textStyle: String? = null,
    val showDisclosure: Boolean,
    override val padding: Padding,
) : Widget, HasPadding, HasText

data class ToggleItem(
    override val id: String,
    override val text: String,
    val key: String,
    override val padding: Padding,
) : Widget, HasPadding, HasText

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

interface HasPadding {

    val padding: Padding
}

@JvmInline
value class ProjectId(val id: String)
