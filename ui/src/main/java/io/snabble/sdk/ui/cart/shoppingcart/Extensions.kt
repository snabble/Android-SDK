package io.snabble.sdk.ui.cart.shoppingcart

import io.snabble.sdk.checkout.PriceModifier
import java.math.BigDecimal

fun PriceModifier.convertPriceModifier(
    amount: Int,
    weightedUnit: String?,
    referencedUnit: String?
): BigDecimal {
    val factor = io.snabble.sdk.Unit.getConversionFactor(weightedUnit, referencedUnit)
    return BigDecimal(amount) * BigDecimal(price) / BigDecimal(factor)
}
