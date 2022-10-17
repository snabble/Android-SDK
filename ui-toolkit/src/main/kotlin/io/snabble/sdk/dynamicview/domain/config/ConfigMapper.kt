package io.snabble.sdk.dynamicview.domain.config

import android.content.Context
import io.snabble.sdk.dynamicview.data.ButtonDto
import io.snabble.sdk.dynamicview.data.ConfigurationDto
import io.snabble.sdk.dynamicview.data.ConnectWlanDto
import io.snabble.sdk.dynamicview.data.CustomerCardDto
import io.snabble.sdk.dynamicview.data.DynamicConfigDto
import io.snabble.sdk.dynamicview.data.ImageDto
import io.snabble.sdk.dynamicview.data.InformationDto
import io.snabble.sdk.dynamicview.data.LocationPermissionDto
import io.snabble.sdk.dynamicview.data.PurchasesDto
import io.snabble.sdk.dynamicview.data.SectionDto
import io.snabble.sdk.dynamicview.data.SeeAllStoresDto
import io.snabble.sdk.dynamicview.data.StartShoppingDto
import io.snabble.sdk.dynamicview.data.TextDto
import io.snabble.sdk.dynamicview.data.ToggleDto
import io.snabble.sdk.dynamicview.data.WidgetDto
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.Configuration
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.ProjectId
import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.domain.model.Widget
import io.snabble.sdk.dynamicview.utils.toPadding
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId
import io.snabble.sdk.utils.resolveResourceString

internal interface ConfigMapper {

    fun mapDtoToItems(dynamicConfigDto: DynamicConfigDto): DynamicConfig
}

internal class ConfigMapperImpl(private val context: Context) : ConfigMapper {

    override fun mapDtoToItems(dynamicConfigDto: DynamicConfigDto): DynamicConfig = DynamicConfig(
        configuration = dynamicConfigDto.configuration.toConfiguration(),
        widgets = dynamicConfigDto.widgets.toWidgets()
    )

    private fun ConfigurationDto.toConfiguration(): Configuration = Configuration(
        image = context.resolveImageId(image),
        style = style,
        padding = padding.toPadding()
    )

    private fun List<WidgetDto>.toWidgets(): List<Widget> = map { widget ->
        with(widget) {
            when (this) {
                is ButtonDto -> toButton()
                is ConnectWlanDto -> toConnectWlan()
                is CustomerCardDto -> toCustomCardItem()
                is ImageDto -> toImage()
                is InformationDto -> toInformation()
                is LocationPermissionDto -> toLocationPermission()
                is PurchasesDto -> toPurchases()
                is SectionDto -> toSection()
                is SeeAllStoresDto -> toSeeAllStores()
                is StartShoppingDto -> toStartShopping()
                is TextDto -> toText()
                is ToggleDto -> toToggle()
            }
        }
    }

    private fun ButtonDto.toButton(): ButtonItem = ButtonItem(
        id = id,
        text = "${context.resolveResourceString(text)}",
        foregroundColor = context.resolveColorId(foregroundColor),
        backgroundColor = context.resolveColorId(backgroundColor),
        padding = padding.toPadding()
    )

    private fun ConnectWlanDto.toConnectWlan(): ConnectWlanItem = ConnectWlanItem(
        id = id,
        padding = padding.toPadding()
    )

    private fun CustomerCardDto.toCustomCardItem(): CustomerCardItem = CustomerCardItem(
        id = id,
        text = "${context.resolveResourceString(text)}",
        image = context.resolveImageId(image),
        padding = padding.toPadding()
    )

    private fun ImageDto.toImage(): ImageItem = ImageItem(
        id = id,
        image = context.resolveImageId(image),
        padding = padding.toPadding()
    )

    private fun InformationDto.toInformation(): InformationItem = InformationItem(
        id = id,
        text = "${context.resolveResourceString(text)}",
        image = context.resolveImageId(image),
        padding = padding.toPadding()
    )

    private fun LocationPermissionDto.toLocationPermission(): LocationPermissionItem =
        LocationPermissionItem(
            id = id,
            padding = padding.toPadding()
        )

    private fun PurchasesDto.toPurchases(): PurchasesItem = PurchasesItem(
        id = id,
        projectId = ProjectId(projectId),
        padding = padding.toPadding()
    )

    private fun SectionDto.toSection(): SectionItem = SectionItem(
        id = id,
        header = "${context.resolveResourceString(header)}",
        items = widgets.toWidgets(),
        padding = padding.toPadding()
    )

    private fun SeeAllStoresDto.toSeeAllStores(): SeeAllStoresItem = SeeAllStoresItem(
        id = id,
        padding = padding.toPadding()
    )

    private fun StartShoppingDto.toStartShopping(): StartShoppingItem = StartShoppingItem(
        id = id,
        padding = padding.toPadding()
    )

    private fun TextDto.toText(): TextItem = TextItem(
        id = id,
        text = "${context.resolveResourceString(text)}",
        textColor = context.getComposeColor(textColor),
        textStyle = textStyle,
        showDisclosure = showDisclosure ?: false,
        padding = padding.toPadding()
    )

    private fun ToggleDto.toToggle(): ToggleItem = ToggleItem(
        id = id,
        text = "${context.resolveResourceString(text)}",
        key = key,
        padding = padding.toPadding()
    )
}
