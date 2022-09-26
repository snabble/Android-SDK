package io.snabble.sdk.ui.widgets.purchase

import io.snabble.sdk.ReceiptInfo

data class Purchase(
    val amount: String,
    val title: String,
    val time: String,
)

// TODO: Make this right!
fun ReceiptInfo.toPurchase() = Purchase(
    amount = price,
    title = shopName,
    time = "$timestamp",
)

