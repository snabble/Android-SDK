package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.SwitchEnvironmentItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.switchEnvironment")
internal data class SwitchEnvironmentDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("values") val values: List<Value>,
) : WidgetDto {

    @Serializable
    data class Value(
        @SerialName("id") val id: String,
        @SerialName("text") val text: String,
    )
}

internal fun SwitchEnvironmentDto.toSwitchEnvironmentItem(
    text: String,
): SwitchEnvironmentItem = SwitchEnvironmentItem(
    id = id,
    text = text,
    values = this.values.map(SwitchEnvironmentDto.Value::toValue)
)

private fun SwitchEnvironmentDto.Value.toValue(): SwitchEnvironmentItem.Value = SwitchEnvironmentItem.Value(id, text)
