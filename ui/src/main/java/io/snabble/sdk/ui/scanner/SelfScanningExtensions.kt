package io.snabble.sdk.ui.scanner

import io.snabble.sdk.Project
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.shoppingcart.ShoppingCart
import java.util.UUID

fun Project.containsReturnDepositVoucher(list: List<ScannedCode>): Pair<CodeTemplate, ScannedCode>? =
    depositReturnVoucherProviders
        .flatMap { it.templates }
        .zip(list)
        .firstOrNull { (codeTemplates, scannedCodes) ->
            scannedCodes.code?.startsWith(codeTemplates.pattern.substringBefore("{")) == true
        }

fun ShoppingCart.insertDepositReturnVoucherItem(codeTemplate: CodeTemplate, scannedCode: ScannedCode) {
    add(
        newItem(
            LineItem(
                id = UUID.randomUUID().toString(),
                amount = 1,
                itemId = codeTemplate.name,
                type = LineItemType.DEPOSIT_RETURN_VOUCHER,
                scannedCode = scannedCode.code
            )
        )
    )
}
