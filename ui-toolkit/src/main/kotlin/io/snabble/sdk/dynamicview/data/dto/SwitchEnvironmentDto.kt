package io.snabble.sdk.dynamicview.data.dto

import android.content.Context
import io.snabble.sdk.dynamicview.domain.model.SwitchEnvironmentItem
import io.snabble.sdk.utils.resolveResourceString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.switchEnvironment")
internal data class SwitchEnvironmentDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("values") val values: List<Value>,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto {

    @Serializable
    data class Value(
        @SerialName("id") val id: String,
        @SerialName("text") val text: String,
    )
}

internal fun SwitchEnvironmentDto.toSwitchEnvironmentItem(
    context: Context,
    text: String,
): SwitchEnvironmentItem = SwitchEnvironmentItem(
    id = id,
    text = text,
    values = values.map { it.toValue(context) },
    padding = padding.toPadding()
)

private fun SwitchEnvironmentDto.Value.toValue(context: Context): SwitchEnvironmentItem.Value =
    SwitchEnvironmentItem.Value(id, "${context.resolveResourceString(text)}")
