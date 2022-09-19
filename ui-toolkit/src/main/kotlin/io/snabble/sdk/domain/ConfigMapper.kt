package io.snabble.sdk.domain

import android.content.Context
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PurchasesDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SpacerDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId
import io.snabble.sdk.data.Widget as WidgetDto

interface ConfigMapper {

    fun mapTo(rootDto: RootDto): Root
}

class ConfigMapperImpl(private val context: Context) : ConfigMapper {

    override fun mapTo(rootDto: RootDto): Root = Root(
        configuration = rootDto.configuration.toConfiguration(),
        widgets = rootDto.widgets.toWidgets(rootDto.configuration.padding)
    )

    private fun ConfigurationDto.toConfiguration(): Configuration = Configuration(
        image = context.resolveImageId(image),
        style = style,
        padding = padding
    )

    private fun List<WidgetDto>.toWidgets(padding: Int): List<Widget> = map { widget ->
        with(widget) {
            when (this) {
                is SpacerDto -> toSpacer()
                is ImageDto -> toImage(padding)
                is TextDto -> toText(padding)
                is ButtonDto -> toButton(padding)
                is InformationDto -> TODO()
                is LocationPermissionDto -> TODO()
                is PurchasesDto -> TODO()
                is SectionDto -> TODO()
                is ToggleDto -> TODO()
            }
        }
    }

    private fun SpacerDto.toSpacer(): SpacerItem = SpacerItem(length = length)

    private fun TextDto.toText(padding: Int): TextItem = TextItem(
        id = id,
        text = text,
        textColorSource = context.getComposeColor(textColorSource),
        textStyleSource = textStyleSource,
        showDisclosure = showDisclosure ?: false,
        padding = padding
    )

    private fun ImageDto.toImage(padding: Int): ImageItem = ImageItem(
        id = id,
        imageSource = context.resolveImageId(imageSource),
        padding = padding
    )

    private fun ButtonDto.toButton(padding: Int): ButtonItem = ButtonItem(
        id = id,
        text = "${context.getResourceString(text)}",
        foregroundColorSource = context.resolveColorId(foregroundColorSource),
        backgroundColorSource = context.resolveColorId(backgroundColorSource),
        padding = padding
    )
}
