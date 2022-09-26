package io.snabble.sdk.domain

import android.content.Context
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.data.PurchasesDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SeeAllStoresDto
import io.snabble.sdk.data.StartShoppingDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import io.snabble.sdk.data.WidgetDto
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
        widgets = rootDto.widgets.toWidgets()
    )

    private fun ConfigurationDto.toConfiguration(): Configuration = Configuration(
        image = context.resolveImageId(image),
        style = style,
    )

    private fun List<WidgetDto>.toWidgets(): List<Widget> = map { widget ->
        with(widget) {
            when (this) {
                is ImageDto -> toImage()
                is TextDto -> toText()
                is ButtonDto -> toButton()
                is InformationDto -> TODO()
                is LocationPermissionDto -> toLocationPermission()
                is PurchasesDto -> toPurchases()
                is SectionDto -> TODO()
                is ToggleDto -> TODO()
                is SeeAllStoresDto -> toSeeAllStores()
                is StartShoppingDto -> toStartShopping()
            }
        }
    }

    private fun TextDto.toText(): TextItem = TextItem(
        id = id,
        text = text,
        textColorSource = context.getComposeColor(textColorSource),
        textStyleSource = textStyleSource,
        showDisclosure = showDisclosure ?: false,
        padding = padding.toPadding()
    )

    private fun ImageDto.toImage(): ImageItem = ImageItem(
        id = id,
        imageSource = context.resolveImageId(imageSource),
        padding = padding.toPadding()
    )

    private fun ButtonDto.toButton(): ButtonItem = ButtonItem(
        id = id,
        text = "${context.getResourceString(text)}",
        foregroundColorSource = context.resolveColorId(foregroundColorSource),
        backgroundColorSource = context.resolveColorId(backgroundColorSource),
        padding = padding.toPadding()
    )

    private fun LocationPermissionDto.toLocationPermission(): LocationPermissionItem =
        LocationPermissionItem(
            id = id,
            padding = padding.toPadding()
        )

    private fun SeeAllStoresDto.toSeeAllStores(): SeeAllStoresItem =
        io.snabble.sdk.domain.SeeAllStoresItem(
            id = id,
            padding = padding.toPadding()
        )

    private fun StartShoppingDto.toStartShopping(): StartShoppingItem = StartShoppingItem(
        id = id,
        padding = padding.toPadding()
    )

    private fun PurchasesDto.toPurchases(): PurchasesItem = PurchasesItem(
        id = id,
        projectId = ProjectId(projectId),
        padding = padding.toPadding()
    )
}

private fun PaddingDto.toPadding() = Padding(start, top, end, bottom)

