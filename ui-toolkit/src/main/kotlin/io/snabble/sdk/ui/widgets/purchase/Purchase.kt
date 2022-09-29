package io.snabble.sdk.ui.widgets.purchase

import io.snabble.sdk.ReceiptInfo

internal data class Purchase(
    val id: String,
    val amount: String,
    val title: String,
    val time: String,
)

internal fun ReceiptInfo.toPurchase(
    timeFormatter: RelativeTimeStringFormatter,
): Purchase = Purchase(
    id = id,
    amount = price,
    title = shopName,
    time = timeFormatter.format(timestamp, nowInMillis = System.currentTimeMillis()),
)
