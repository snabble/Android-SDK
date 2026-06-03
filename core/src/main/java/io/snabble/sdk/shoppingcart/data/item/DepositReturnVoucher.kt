package io.snabble.sdk.shoppingcart.data.item

import androidx.annotation.Keep
import io.snabble.sdk.checkout.LineItem

@Keep
data class DepositReturnVoucher(
    val itemId: String,
    val scannedCode: String,
    val amount: Int = 1,
    val type: ItemType = ItemType.DEPOSIT_RETURN_VOUCHER,
    val lineItems: List<LineItem> = emptyList()
)
