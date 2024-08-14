package io.snabble.sdk.ui.cart.shoppingcart

import io.snabble.sdk.Snabble
import io.snabble.sdk.Unit.convert
import io.snabble.sdk.Unit.fromString
import io.snabble.sdk.checkout.PriceModifier
import java.math.BigDecimal
import java.math.RoundingMode

fun PriceModifier.convertPriceModifier(
    amount: Int,
    weightedUnit: String,
    referencedUnit: String
): Int {
    val convertedValue = convert(BigDecimal(amount), fromString(weightedUnit), fromString(referencedUnit))
    val mode = Snabble.checkedInProject.value?.roundingMode ?: RoundingMode.HALF_UP
    val digits = Snabble.checkedInProject.value?.currency?.defaultFractionDigits ?: 0
    return convertedValue
        .multiply(BigDecimal(price))
        .setScale(digits, mode)
        .toInt()
}
