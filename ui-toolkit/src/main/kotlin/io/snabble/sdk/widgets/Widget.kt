package io.snabble.sdk.widgets

sealed interface Widget {
    var id: Int
    var type: WidgetType
}

enum class WidgetType {
    Text,
    Image,
    Button,
    Information,
    Purchases,
    Section,
    LocationPermission,
    Toggle
}

data class TextModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Text,
    var text: String,
    var textColorSource: String?,
    var textStyleSource: String?,
    var showDisclosure: Boolean?,
    var spacing: Float?
) : Widget

data class ImageModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Image,
    var imageSource: String,
    var spacing: Float?
) : Widget

data class ButtonModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Button,
    var text: String,
    var foregroundColorSource: String?,
    var backgroundColorSource: String?,
    var spacing: Float?
) : Widget

data class InformationModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Information,
    var text: String,
    var ImageSource: String?,
    var hideable: Boolean?,
    var spacing: Float?
) : Widget

data class PurchasesModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Purchases,
    var projectId: String,
    var spacing: Float?
) : Widget

data class ToggleModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Toggle,
    var text: String,
    var key: String,
    var spacing: Float?
) : Widget

data class SectionModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.Section,
    var header: String,
    var items: List<Widget>,
    var spacing: Float?
) : Widget

data class LocationPermissionModel(
    override var id: Int,
    override var type: WidgetType = WidgetType.LocationPermission,
    var spacing: Float?
) : Widget
