package io.snabble.sdk.dynamicview.domain.model

internal data class SwitchEnvironmentItem(
    override val id: String,
    val text: String,
    val values: List<Value>,
) : Widget {

    data class Value(
        val id: String,
        val text: String,
    )
}
