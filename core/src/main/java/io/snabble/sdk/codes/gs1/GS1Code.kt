package io.snabble.sdk.codes.gs1

import io.snabble.sdk.Dimension
import io.snabble.sdk.Unit
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.min

/**
 * Class for parsing GS1 codes
 */
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

    /**
     * Contains all elements found in the GS1 code
     */
    val elements: ArrayList<Element> = ArrayList()

    /**
     * Contains all unknown elements, not recognized by the parser
     */
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
        while (nextElement()) {}
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
                                remainingCode = leftover
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

    private fun firstValue(prefix: String): String? =
        elements.firstOrNull {
            it.identifier.prefix.startsWith(prefix)
        }?.values?.firstOrNull()

    private fun firstDecimal(prefix: String): BigDecimal? =
        elements.firstOrNull { it.identifier.prefix.startsWith(prefix) }?.decimal

    private fun firstElement(prefix: String): Element? =
        elements.firstOrNull { it.identifier.prefix.startsWith(prefix) }

    private fun BigDecimal.trim(): BigDecimal {
        val df = DecimalFormat()
        df.maximumFractionDigits = 2
        df.minimumFractionDigits = 0
        df.isGroupingUsed = false
        df.isParseBigDecimal = true
        return df.parse(df.format(this)) as BigDecimal
    }

    /**
     * Get the weight if contained in the gs1 code, converted to [Unit].
     */
    fun getWeight(unit: Unit): BigDecimal? {
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

    /**
     * Get the weight if contained in the gs1 code, converted to grams.
     */
    val weight: Int?
        get() = getWeight(Unit.GRAM)?.toInt()

    /**
     * Get the length if contained in the gs1 code, converted to [Unit].
     */
    fun getLength(unit: Unit): BigDecimal? {
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

    /**
     * Get the length if contained in the gs1 code, converted to millimeters.
     */
    val length: Int?
        get() = getLength(Unit.MILLIMETER)?.toInt()

    /**
     * Get the area if contained in the gs1 code, converted to [Unit].
     */
    fun getArea(unit: Unit): BigDecimal? {
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

    /**
     * Get the area if contained in the gs1 code, converted to square centimeters.
     */
    val area: Int?
        get() = getArea(Unit.SQUARE_CENTIMETER)?.toInt()

    /**
     * Get the liters if contained in the gs1 code, converted to [Unit].
     */
    fun getLiters(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.VOLUME) {
            val liters = firstDecimal("315")
            if (liters != null) {
                return when (unit) {
                    Unit.LITER -> liters
                    Unit.DECILITER -> liters * 10.toBigDecimal()
                    Unit.CENTILITER -> liters * 100.toBigDecimal()
                    Unit.MILLILITER -> liters * 1000.toBigDecimal()
                    else -> null
                }?.trim()
            }
        }

        return null
    }

    /**
     * Get the liters if contained in the gs1 code, converted to milliliters.
     */
    val liters: Int?
        get() = getLiters(Unit.MILLILITER)?.toInt()

    /**
     * Get the volume if contained in the gs1 code, converted to [Unit].
     */
    fun getVolume(unit: Unit): BigDecimal? {
        if (unit.dimension == Dimension.CAPACITY) {
            val volume = firstDecimal("316")
            if (volume != null) {
                return when (unit) {
                    Unit.CUBIC_METER -> volume
                    Unit.CUBIC_CENTIMETER -> volume * 1_000_000.toBigDecimal()
                    else -> null
                }?.trim()
            }
        }

        return null
    }

    /**
     * Get the volume if contained in the gs1 code, converted to cubic centimeters.
     */
    val volume: Int?
        get() = getVolume(Unit.CUBIC_CENTIMETER)?.toInt()

    /**
     * Get the GTIN that is contained in the GS1 code for identifying the product.
     */
    val gtin: String?
        get() = firstValue("01")

    /**
     * Class representing a parsed price in a gs1 code
     */
    data class Price(
        val price: BigDecimal,
        val currencyCode: String?
    )

    /**
     * Get the price if contained in the gs1 code
     */
    val price: Price?
        get() {
            val p1 = firstDecimal("392")
            if (p1 != null) {
                return Price(p1, null)
            }

            val p2 = firstElement("393")
            if (p2 != null) {
                p2.decimal?.let { price ->
                    return Price(price, p2.values[0])
                }
            }

            return null
        }

    /**
     * Get the price if contained in the gs1 code, rounding to a set number of digits
     */
    fun getPrice(digits: Int, roundingMode: RoundingMode): Price? =
        price?.let { price ->
            Price(
                price = price.price.setScale(digits, roundingMode),
                currencyCode = price.currencyCode
            )
        }

    /**
     * Get the amount if contained in the gs1 code
     */
    val amount: Int?
        get() = firstValue("30")?.toIntOrNull()

    /**
     * Get the encoded data from gs1 code, if contained.
     */
    fun getEmbeddedData(encodingUnit: Unit?, digits: Int?, roundingMode: RoundingMode?): BigDecimal? =
        when (encodingUnit?.dimension) {
            null -> null
            Dimension.VOLUME -> getLiters(encodingUnit)
            Dimension.CAPACITY -> getVolume(encodingUnit)
            Dimension.AREA -> getArea(encodingUnit)
            Dimension.DISTANCE -> getLength(encodingUnit)
            Dimension.MASS -> getWeight(encodingUnit)
            Dimension.COUNT -> this.amount?.let { BigDecimal(it) }
            Dimension.AMOUNT -> {
                if (digits != null && roundingMode != null) {
                    this.getPrice(digits, roundingMode)?.price
                } else {
                    this.price?.price
                }
            }
        }
}