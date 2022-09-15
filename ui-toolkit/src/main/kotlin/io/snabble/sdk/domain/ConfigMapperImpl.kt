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
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId
import io.snabble.sdk.data.Widget as WidgetDto

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
        padding = padding
    )

    private fun List<WidgetDto>.toWidgets(): List<Widget> = map { widget ->
        when (widget) {
            is ImageDto -> widget.toImage()
            is TextDto -> widget.toText()
            is ButtonDto -> TODO()
            is InformationDto -> TODO()
            is LocationPermissionDto -> TODO()
            is PurchasesDto -> TODO()
            is SectionDto -> TODO()
            is ToggleDto -> TODO()
        }
    }

    private fun ImageDto.toImage(): Image = Image(
        id = id,
        imageSource = context.resolveImageId(imageSource),
        spacing = spacing
    )

    private fun TextDto.toText(): Text = Text(
        id = id,
        text = text,
        textColorSource = context.resolveColorId(textColorSource),
        textStyleSource = textStyleSource,
        showDisclosure = showDisclosure ?: false,
        spacing = spacing
    )
}
