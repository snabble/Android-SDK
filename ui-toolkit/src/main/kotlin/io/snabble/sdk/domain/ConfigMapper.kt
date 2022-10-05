package io.snabble.sdk.domain

import android.content.Context
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ConnectWifiDto
import io.snabble.sdk.data.CustomerCardDto
import io.snabble.sdk.data.DynamicConfigDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PurchasesDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SeeAllStoresDto
import io.snabble.sdk.data.StartShoppingDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import io.snabble.sdk.data.WidgetDto
import io.snabble.sdk.ui.toPadding
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId

interface ConfigMapper {

    fun mapTo(dynamicConfigDto: DynamicConfigDto): DynamicConfig
}

class ConfigMapperImpl(private val context: Context) : ConfigMapper {

    override fun mapTo(dynamicConfigDto: DynamicConfigDto): DynamicConfig = DynamicConfig(
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
                is ConnectWifiDto -> toConnectWifi()
                is CustomerCardDto -> toCustomCardItem()
                is ImageDto -> toImage()
                is InformationDto -> toInformation()
                is LocationPermissionDto -> toLocationPermission()
                is PurchasesDto -> toPurchases()
                is SectionDto -> TODO()
                is SeeAllStoresDto -> toSeeAllStores()
                is StartShoppingDto -> toStartShopping()
                is TextDto -> toText()
                is ToggleDto -> toToggle()
            }
        }
    }

    private fun ButtonDto.toButton(): ButtonItem = ButtonItem(
        id = id,
        text = "${context.getResourceString(text)}",
        foregroundColorSource = context.resolveColorId(foregroundColorSource),
        backgroundColorSource = context.resolveColorId(backgroundColorSource),
        padding = padding.toPadding()
    )

    private fun ConnectWifiDto.toConnectWifi(): ConnectWifiItem = ConnectWifiItem(
        id = id,
        padding = padding.toPadding()
    )

    private fun CustomerCardDto.toCustomCardItem(): CustomerCardItem = CustomerCardItem(
        id = id,
        text = text,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding()
    )

    private fun ImageDto.toImage(): ImageItem = ImageItem(
        id = id,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding()
    )

    private fun InformationDto.toInformation(): InformationItem = InformationItem(
        id = id,
        text = text,
        imageSource = context.resolveImageId(imageSource),
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
        text = text,
        textColorSource = context.getComposeColor(textColorSource),
        textStyleSource = textStyleSource,
        showDisclosure = showDisclosure ?: false,
        padding = padding.toPadding()
    )

    private fun ToggleDto.toToggle(): ToggleItem = ToggleItem(
        id = id,
        text = text,
        key = key,
        padding = padding.toPadding()
    )
}
