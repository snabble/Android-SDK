package io.snabble.sdk.domain

import android.content.Context
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ConnectWifiDto
import io.snabble.sdk.data.CustomerCardDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PurchasesDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SeeAllStoresDto
import io.snabble.sdk.data.StartShoppingDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import io.snabble.sdk.data.WidgetDto
import io.snabble.sdk.ui.plus
import io.snabble.sdk.ui.toPadding
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId

interface ConfigMapper {

    fun mapTo(rootDto: RootDto): Root
}

class ConfigMapperImpl(private val context: Context) : ConfigMapper {

    override fun mapTo(rootDto: RootDto): Root = Root(
        configuration = rootDto.configuration.toConfiguration(),
        widgets = rootDto.widgets.toWidgets(rootDto.configuration.padding.toPadding())
    )

    private fun ConfigurationDto.toConfiguration(): Configuration = Configuration(
        image = context.resolveImageId(image),
        style = style,
        padding = padding.toPadding()
    )

    private fun List<WidgetDto>.toWidgets(outerPadding: Padding): List<Widget> = map { widget ->
        with(widget) {
            when (this) {
                is ImageDto -> toImage(outerPadding)
                is TextDto -> toText(outerPadding)
                is ButtonDto -> toButton(outerPadding)
                is InformationDto -> toInformation(outerPadding)
                is CustomerCardDto -> toCustomCardItem(outerPadding)
                is LocationPermissionDto -> toLocationPermission(outerPadding)
                is SeeAllStoresDto -> toSeeAllStores(outerPadding)
                is StartShoppingDto -> toStartShopping(outerPadding)
                is ConnectWifiDto -> toConnectWifi(outerPadding)
                is PurchasesDto -> TODO()
                is SectionDto -> TODO()
                is ToggleDto -> TODO()
            }
        }
    }

    private fun TextDto.toText(outerPadding: Padding): TextItem = TextItem(
        id = id,
        text = text,
        textColorSource = context.getComposeColor(textColorSource),
        textStyleSource = textStyleSource,
        showDisclosure = showDisclosure ?: false,
        padding = padding.toPadding() + outerPadding
    )

    private fun ImageDto.toImage(outerPadding: Padding): ImageItem = ImageItem(
        id = id,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding() + outerPadding
    )

    private fun ButtonDto.toButton(outerPadding: Padding): ButtonItem = ButtonItem(
        id = id,
        text = "${context.getResourceString(text)}",
        foregroundColorSource = context.resolveColorId(foregroundColorSource),
        backgroundColorSource = context.resolveColorId(backgroundColorSource),
        padding = padding.toPadding() + outerPadding
    )

    private fun InformationDto.toInformation(outerPadding: Padding): InformationItem = InformationItem(
        id = id,
        text = text,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding() + outerPadding
    )

    private fun CustomerCardDto.toCustomCardItem(outerPadding: Padding): CustomerCardItem = CustomerCardItem(
        id = id,
        text = text,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding() + outerPadding
    )

    private fun LocationPermissionDto.toLocationPermission(outerPadding: Padding): LocationPermissionItem =
        LocationPermissionItem(
            id = id,
            padding = padding.toPadding() + outerPadding
        )

    private fun SeeAllStoresDto.toSeeAllStores(outerPadding: Padding): SeeAllStoresItem = SeeAllStoresItem(
        id = id,
        padding = padding.toPadding() + outerPadding
    )

    private fun StartShoppingDto.toStartShopping(outerPadding: Padding): StartShoppingItem = StartShoppingItem(
        id = id,
        padding = padding.toPadding() + outerPadding
    )

    private fun ConnectWifiDto.toConnectWifi(outerPadding: Padding): ConnectWifiItem = ConnectWifiItem(
        id = id,
        padding = padding.toPadding() + outerPadding
    )
}
