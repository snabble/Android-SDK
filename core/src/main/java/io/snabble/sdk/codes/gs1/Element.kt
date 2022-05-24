package io.snabble.sdk.codes.gs1

import java.math.BigDecimal

/**
 * Class for holding gs1 code blocks
 */
class Element(val identifier: ApplicationIdentifier,
              val values: List<String>) {
    val decimal: BigDecimal?
        get() {
            if (identifier.prefix.length == 4) {
                val decimalPoints = Character.getNumericValue(identifier.prefix[3])
                val divisor = BigDecimal(10).pow(decimalPoints)
                val v = values.lastOrNull()
                if (v != null) {
                    return if (divisor.toInt() != 0) {
                        BigDecimal(v).divide(divisor)
                    } else {
                        BigDecimal(v)
                    }
                }
            }

            return null
        }
}