package io.snabble.sdk.dynamicview.data.dto.mapper

import android.content.Context
import io.snabble.sdk.dynamicview.data.dto.AppUserIdDto
import io.snabble.sdk.dynamicview.data.dto.ButtonDto
import io.snabble.sdk.dynamicview.data.dto.ClientIdDto
import io.snabble.sdk.dynamicview.data.dto.ConfigurationDto
import io.snabble.sdk.dynamicview.data.dto.ConnectWlanDto
import io.snabble.sdk.dynamicview.data.dto.CustomerCardDto
import io.snabble.sdk.dynamicview.data.dto.DevSettingsDto
import io.snabble.sdk.dynamicview.data.dto.DynamicConfigDto
import io.snabble.sdk.dynamicview.data.dto.ImageDto
import io.snabble.sdk.dynamicview.data.dto.InformationDto
import io.snabble.sdk.dynamicview.data.dto.LocationPermissionDto
import io.snabble.sdk.dynamicview.data.dto.PurchasesDto
import io.snabble.sdk.dynamicview.data.dto.SectionDto
import io.snabble.sdk.dynamicview.data.dto.SeeAllStoresDto
import io.snabble.sdk.dynamicview.data.dto.SsidProvider
import io.snabble.sdk.dynamicview.data.dto.StartShoppingDto
import io.snabble.sdk.dynamicview.data.dto.SwitchEnvironmentDto
import io.snabble.sdk.dynamicview.data.dto.TextDto
import io.snabble.sdk.dynamicview.data.dto.ToggleDto
import io.snabble.sdk.dynamicview.data.dto.VersionDto
import io.snabble.sdk.dynamicview.data.dto.WidgetDto
import io.snabble.sdk.dynamicview.data.dto.toAppUserId
import io.snabble.sdk.dynamicview.data.dto.toButton
import io.snabble.sdk.dynamicview.data.dto.toClientId
import io.snabble.sdk.dynamicview.data.dto.toConnectWlan
import io.snabble.sdk.dynamicview.data.dto.toCustomCardItem
import io.snabble.sdk.dynamicview.data.dto.toDevSettingsItem
import io.snabble.sdk.dynamicview.data.dto.toImage
import io.snabble.sdk.dynamicview.data.dto.toInformation
import io.snabble.sdk.dynamicview.data.dto.toLocationPermission
import io.snabble.sdk.dynamicview.data.dto.toPadding
import io.snabble.sdk.dynamicview.data.dto.toPurchases
import io.snabble.sdk.dynamicview.data.dto.toSeeAllStores
import io.snabble.sdk.dynamicview.data.dto.toStartShopping
import io.snabble.sdk.dynamicview.data.dto.toSwitchEnvironmentItem
import io.snabble.sdk.dynamicview.data.dto.toText
import io.snabble.sdk.dynamicview.data.dto.toToggle
import io.snabble.sdk.dynamicview.data.dto.toVersion
import io.snabble.sdk.dynamicview.domain.model.Configuration
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.Widget
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId
import io.snabble.sdk.utils.resolveResourceString

internal interface ConfigMapper {

    fun mapDtoToItems(dynamicConfigDto: DynamicConfigDto): DynamicConfig
}

internal class ConfigMapperImpl(private val context: Context, private val ssidProvider: SsidProvider) : ConfigMapper {

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
                is AppUserIdDto -> toAppUserId()

                is ButtonDto -> toButton(
                    text = "${context.resolveResourceString(text)}",
                    foregroundColor = context.resolveColorId(foregroundColor),
                    backgroundColor = context.resolveColorId(backgroundColor)
                )

                is ClientIdDto -> toClientId()

                is ConnectWlanDto -> toConnectWlan(ssidProvider)

                is CustomerCardDto -> toCustomCardItem(
                    text = "${context.resolveResourceString(text)}",
                    image = context.resolveImageId(image)
                )

                is DevSettingsDto -> toDevSettingsItem(text = "${context.resolveResourceString(text)}")

                is ImageDto -> toImage(image = context.resolveImageId(image))

                is InformationDto -> toInformation(
                    text = "${context.resolveResourceString(text)}",
                    image = context.resolveImageId(image)
                )

                is LocationPermissionDto -> toLocationPermission()

                is SwitchEnvironmentDto -> toSwitchEnvironmentItem(
                    context = context,
                    text = "${context.resolveResourceString(text)}"
                )

                is PurchasesDto -> toPurchases()

                is SectionDto -> toSection()

                is SeeAllStoresDto -> toSeeAllStores()

                is StartShoppingDto -> toStartShopping()

                is TextDto -> toText(
                    text = "${context.resolveResourceString(text)}",
                    textColor = context.getComposeColor(textColor)
                )

                is ToggleDto -> toToggle(text = "${context.resolveResourceString(text)}")

                is VersionDto -> toVersion()
            }
        }
    }

    private fun SectionDto.toSection(): SectionItem = SectionItem(
        id = id,
        header = "${context.resolveResourceString(header)}",
        items = widgets.toWidgets(),
        padding = padding.toPadding()
    )
}
