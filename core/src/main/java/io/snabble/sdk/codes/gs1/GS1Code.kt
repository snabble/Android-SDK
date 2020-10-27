package io.snabble.sdk.codes.gs1

import io.snabble.sdk.Dimension
import io.snabble.sdk.Unit
import java.math.BigDecimal
import kotlin.math.min

class GS1Code(val code: String) {
    companion object {
        const val GS = "\u001D"

        private val symbologyIdentifiers = listOf(
                "]C1",  // = GS1-128
                "]e0",  // = GS1 DataBar
                "]d2",  // = GS1 DataMatrix
                "]Q3",  // = GS1 QR Code
                "]J1"   // = GS1 DotCode
        )
    }

    val identifiers: ArrayList<Element> = ArrayList()
    val skipped: ArrayList<String> = ArrayList()

    private var remainingCode: String

    init {
        remainingCode = code
        parse()
    }

    private fun parse() {
        symbologyIdentifiers.forEach {
            remainingCode = remainingCode.removePrefix(it)
        }

        @Suppress("ControlFlowWithEmptyBody")
        while (nextElement()){}
    }

    private fun nextElement(): Boolean {
        while (remainingCode.startsWith(GS) && remainingCode.isNotEmpty()) {
            remainingCode = remainingCode.removePrefix(GS)
        }

        if (remainingCode.length >= 2) {
            val prefix = remainingCode.substring(0, 2)
            val elementLength = ApplicationIdentifier.elementLength(prefix)
            val elementString = if (elementLength > 0) {
                remainingCode.substring(0, min(remainingCode.length, elementLength))
            } else {
                remainingCode.substringBefore(GS)
            }

            if (elementString.isNotEmpty()) {
                ApplicationIdentifier.byPrefix(prefix)?.forEach { ai ->
                    val remaining = remainingCode.removePrefix(prefix)
                    if (remaining.startsWith(ai.additionalIdentifier ?: "")) {
                        val contentLength = ai.contentLength

                        remainingCode = if (contentLength > 0) {
                            remainingCode.substringAfter(elementString, "")
                        } else {
                            remainingCode.substringAfter(GS, "")
                        }

                        val result = Regex(ai.regex).find(elementString)
                        if (result != null) {
                            identifiers.add(Element(ai, result.groupValues.drop(1)))
                            val leftover = elementString.removeRange(result.range)
                            if (leftover.isNotEmpty()) {
                                skipped.add(leftover)
                            }
                        } else {
                            skipped.add(elementString)
                        }
                        return true
                    }
                }

                skipped.add(elementString)
                remainingCode = remainingCode.substringAfter(elementString, "")
                return true
            } else {
                return true
            }
        }

        return false
    }

    private fun firstValue(prefix: String): String? {
        return identifiers.firstOrNull{
            it.identifier.prefix.startsWith(prefix)
        }?.values?.firstOrNull()
    }

    fun weight(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.MASS) {
            val weight = firstValue("310")?.toIntOrNull()
            if (weight != null) {
                return when (unit) {
                    Unit.KILOGRAM -> BigDecimal(weight)
                    Unit.HECTOGRAM -> BigDecimal(weight * 10)
                    Unit.DECAGRAM -> BigDecimal(weight * 100)
                    Unit.GRAM -> BigDecimal(weight * 1000)
                    else -> return null
                }
            }
        }

        return null
    }

    val weight: Int?
        get() {
            return weight(Unit.GRAM)?.toInt()
        }
}