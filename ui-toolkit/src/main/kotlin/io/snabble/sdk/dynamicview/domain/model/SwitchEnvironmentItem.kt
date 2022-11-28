package io.snabble.sdk.dynamicview.domain.model

data class SwitchEnvironmentItem(
    override val id: String,
    val text: String,
    val values: List<Value>,
    val padding: Padding,
) : Widget {

    data class Value(
        val id: String,
        val text: String,
    )
}
