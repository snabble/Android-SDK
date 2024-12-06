package io.snabble.sdk.ui.scanner

import io.snabble.sdk.Project
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.DepositReturnVoucher

fun Project.containsReturnDepositVoucher(list: List<ScannedCode>): Pair<CodeTemplate, String>? =
    depositReturnVoucherProviders
        .flatMap { it.templates }
        .zip(list.mapNotNull { it.code })
        .firstOrNull { (codeTemplates, scannedCodes) ->
            codeTemplates.match(scannedCodes).buildCode() != null
        }

fun ShoppingCart.insertDepositReturnVoucherItem(codeTemplate: CodeTemplate, scannedCode: String) {
    add(
        newItem(
            DepositReturnVoucher(
                itemId = codeTemplate.name,
                scannedCode = scannedCode
            )
        )
    )
}
