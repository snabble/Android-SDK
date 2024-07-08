package io.snabble.sdk.ui.cart.shoppingcart

import io.snabble.sdk.Unit.getConversionDivisor
import io.snabble.sdk.checkout.PriceModifier
import java.math.BigDecimal

fun PriceModifier.convertPriceModifier(
    amount: Int,
    weightedUnit: String?,
    referencedUnit: String?
): BigDecimal {
    val divisor = getConversionDivisor(weightedUnit, referencedUnit)
    return BigDecimal(amount) * BigDecimal(price) / BigDecimal(divisor)
}
