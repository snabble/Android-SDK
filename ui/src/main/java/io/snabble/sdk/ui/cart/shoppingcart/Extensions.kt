package io.snabble.sdk.ui.cart.shoppingcart

import io.snabble.sdk.Snabble
import io.snabble.sdk.Unit.convert
import io.snabble.sdk.Unit.fromString
import io.snabble.sdk.checkout.PriceModifier
import java.math.BigDecimal

fun PriceModifier.convertPriceModifier(
    amount: Int,
    weightedUnit: String?,
    referencedUnit: String?
): Int {
    val factor = convert(BigDecimal(amount), fromString(weightedUnit), fromString(referencedUnit))
    val mode = Snabble.checkedInProject.value?.roundingMode
    val digits = Snabble.checkedInProject.value?.currency?.defaultFractionDigits ?: 0
    return factor
        .multiply(BigDecimal(price))
        .setScale(digits, mode)
        .toInt()
}
