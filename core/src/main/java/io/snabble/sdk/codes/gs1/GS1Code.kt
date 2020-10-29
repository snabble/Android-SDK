package io.snabble.sdk.codes.gs1

import io.snabble.sdk.Dimension
import io.snabble.sdk.Unit
import java.math.BigDecimal
import java.text.DecimalFormat
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

    val elements: ArrayList<Element> = ArrayList()
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
                            elements.add(Element(ai, result.groupValues.drop(1)))
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
        return elements.firstOrNull{
            it.identifier.prefix.startsWith(prefix)
        }?.values?.firstOrNull()
    }

    private fun firstDecimal(prefix: String): BigDecimal? {
        return elements.firstOrNull{ it.identifier.prefix.startsWith(prefix) }?.decimal
    }

    private fun BigDecimal.trim(): BigDecimal {
        val df = DecimalFormat()
        df.maximumFractionDigits = 2
        df.minimumFractionDigits = 0
        df.isGroupingUsed = false
        df.isParseBigDecimal = true
        return df.parse(df.format(this)) as BigDecimal
    }

    fun weight(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.MASS) {
            val weight = firstDecimal("310")
            if (weight != null) {
                return when (unit) {
                    Unit.KILOGRAM -> weight
                    Unit.HECTOGRAM -> weight * 10.toBigDecimal()
                    Unit.DECAGRAM -> weight * 100.toBigDecimal()
                    Unit.GRAM -> weight * 1000.toBigDecimal()
                    else -> null
                }?.trim()
            }
        }

        return null
    }

    val weight: Int?
        get() {
            return weight(Unit.GRAM)?.toInt()
        }

    fun length(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.DISTANCE) {
            val length = firstDecimal("311")
            if (length != null) {
                return when (unit) {
                    Unit.METER -> length
                    Unit.DECIMETER -> length * 10.toBigDecimal()
                    Unit.CENTIMETER -> length * 100.toBigDecimal()
                    Unit.MILLIMETER -> length * 1000.toBigDecimal()
                    else -> null
                }?.trim()
            }
        }

        return null
    }

    val length: Int?
        get() {
            return length(Unit.MILLIMETER)?.toInt()
        }

    fun area(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.AREA) {
            val area = firstDecimal("314")
            if (area != null) {
                return when (unit) {
                    Unit.SQUARE_METER -> area
                    Unit.SQUARE_METER_TENTH -> area * 10.toBigDecimal()
                    Unit.SQUARE_DECIMETER -> area * 100.toBigDecimal()
                    Unit.SQUARE_DECIMETER_TENTH -> area * 1000.toBigDecimal()
                    Unit.SQUARE_CENTIMETER -> area * 10000.toBigDecimal()
                    else -> null
                }?.trim()
            }
        }

        return null
    }

    val area: Int?
        get() {
            return area(Unit.SQUARE_CENTIMETER)?.toInt()
        }
}