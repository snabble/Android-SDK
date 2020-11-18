package io.snabble.sdk

import io.snabble.sdk.codes.gs1.GS1Code
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class GS1CodeTest {
    companion object {
        const val GS = "\u001D"
    }

    @Test
    fun testInvalidCodes() {
        val code1 = GS1Code("")
        Assert.assertEquals(code1.elements.size, 0)
        Assert.assertEquals(code1.skipped.size, 0)

        val code2 = GS1Code("${GS}${GS}")
        Assert.assertEquals(code2.elements.size, 0)
        Assert.assertEquals(code1.skipped.size, 0) // FIXME: different than in iOS SDK - not really important

        val code3 = GS1Code("asdfghjklöä")
        Assert.assertEquals(code3.elements.size, 0)
        Assert.assertEquals(code3.skipped.size, 1)

        val code4 = GS1Code("   0100000000000000    ")
        Assert.assertEquals(code4.elements.size, 0)
        Assert.assertEquals(code4.skipped.size, 1)

        // invalid lot number
        check("10invälid", mapOf("10" to "inv"), listOf("älid"))

        // invalid gtin
        check("010000000000000", null, listOf("010000000000000"))

        // another invalid gtin
        check("01ABCDEFGHIJKLMN", null, listOf("01ABCDEFGHIJKLMN"))

        // empty gtin is also invalid
        check("01", null, listOf("01"))
    }

    @Test
    fun testCodes() {
        // gtin, net weight, empty lot
        check("0102658960000004310300464610",
                mapOf("01" to "02658960000004", "3103" to "004646", "10" to ""))

        // gtin, net weight, empty lot with gratuituous separators
        check("${GS}0102658960000004${GS}3103004646${GS}10${GS}",
                mapOf("01" to "02658960000004", "3103" to "004646", "10" to ""), )

        // gtin, net weight, lot with more gratuituous separators
        check("01026589600000043103004646${GS}${GS}10test${GS}${GS}${GS}${GS}",
                mapOf("01" to "02658960000004", "3103" to "004646", "10" to "test"))

        // gtin, net weight, lot, dangerous goods flag plus two invalid AIs (3109 and 4321)
        check("0102658960000004${GS}3103004646${GS}10HALLO${GS}3109kkkkkk43217${GS}43210",
                mapOf("01" to "02658960000004", "3103" to "004646", "10" to "HALLO", "4321" to "0"),
                listOf("3109kkkkkk", "43217"))

        // valid lot number
        check("10valid", mapOf( "10" to "valid"))

        // test with QR code prefix
        check("]Q30100000000000000${GS}10FOO", mapOf( "01" to "00000000000000", "10" to "FOO"))
    }

    @Test
    fun testSingle() {
        // check("010000000000000", null, listOf("010000000000000"))
        check("010000000000000_${GS}01123", null, listOf("010000000000000_", "01123"))
    }

    @Test
    fun testMultivalue() {
        // amount payable, currency=111, value=0123456
        val code1 = GS1Code("39101110123456")
        Assert.assertEquals(code1.elements[0].values.size, 2)
        Assert.assertEquals(code1.elements[0].values[0], "111")
        Assert.assertEquals(code1.elements[0].values[1], "0123456")

        // amount payable, invalid data
        val code2 = GS1Code("3910aaabbbb")
        Assert.assertEquals(code2.elements.size, 0)
        Assert.assertEquals(code2.skipped.size, 1)
        Assert.assertEquals(code2.skipped[0], "3910aaabbbb")
    }

    @Test
    fun testDecimal() {
        // net weight in kg, no decimal digits
        val code1 = GS1Code("3100000042")
        Assert.assertEquals(code1.elements[0].values.size, 1)
        Assert.assertEquals(code1.elements[0].values[0], "000042")
        Assert.assertEquals(code1.elements[0].decimal, BigDecimal("42"))

        // net weight in kg, 3 decimal digits
        val code2 = GS1Code("3103001042")
        Assert.assertEquals(code2.elements[0].values.size, 1)
        Assert.assertEquals(code2.elements[0].values[0], "001042")
        Assert.assertEquals(code2.elements[0].decimal, BigDecimal("1.042"))
    }

    @Test
    fun testWeight() {
        // net weight in kg, no decimal digits: 1kg
        val code1 = GS1Code("3100000001")
        val weight1 = code1.getWeight(Unit.KILOGRAM)
        Assert.assertEquals(weight1, 1.toBigDecimal())

        // net weight in kg, no decimal digits: 1kg
        val code2 = GS1Code("3100000001")
        val weight2 = code2.getWeight(Unit.GRAM)
        Assert.assertEquals(weight2, 1000.toBigDecimal())

        // net weight in kg, 3 decimal digits: 1.02kg
        val code3 = GS1Code("3103001020")
        val weight3 = code3.getWeight(Unit.GRAM)
        Assert.assertEquals(weight3, 1020.toBigDecimal())

        // net weight in kg, 2 decimal digits: 1.23kg
        val code4 = GS1Code("3102000123")
        val weight4 = code4.getWeight(Unit.DECAGRAM)
        Assert.assertEquals(weight4, 123.toBigDecimal())

        // net weight in kg, 2 decimal digits: 1.24kg
        val code5 = GS1Code("3102000124")
        val weight5 = code5.getWeight(Unit.HECTOGRAM)
        Assert.assertEquals(weight5, 12.4.toBigDecimal())

        Assert.assertEquals(GS1Code("3100000001").weight, 1000)
        Assert.assertEquals(GS1Code("3101000011").weight, 1100)
        Assert.assertEquals(GS1Code("3102000124").weight, 1240)
        Assert.assertEquals(GS1Code("3103001001").weight, 1001)

        Assert.assertNull(GS1Code("3102000124").getWeight(Unit.LITER))
        Assert.assertNull(GS1Code("3902000124").getWeight(Unit.KILOGRAM))
    }

    @Test
    fun testLength() {
        // net length in m, no decimal digits: 1m
        val code1 = GS1Code("3110000001")
        val length1 = code1.getLength(Unit.METER)
        Assert.assertEquals(length1, BigDecimal(1))

        // net length in m, no decimal digits: 1m
        val code2 = GS1Code("3110000001")
        val length2 = code2.getLength(Unit.MILLIMETER)
        Assert.assertEquals(length2, BigDecimal(1000))

        // net length in m, 3 decimal digits: 1.02m
        val code3 = GS1Code("3113001020")
        val length3 = code3.getLength(Unit.MILLIMETER)
        Assert.assertEquals(length3, BigDecimal(1020))

        // net length in m, 2 decimal digits: 1.23m
        val code4 = GS1Code("3112000123")
        val length4 = code4.getLength(Unit.CENTIMETER)
        Assert.assertEquals(length4, BigDecimal(123))

        // net length in m, 2 decimal digits: 1.24m
        val code5 = GS1Code("3112000124")
        val length5 = code5.getLength(Unit.DECIMETER)
        Assert.assertEquals(length5, BigDecimal("12.4"))

        Assert.assertEquals(GS1Code("3110000001").length, 1000)
        Assert.assertEquals(GS1Code("3111000011").length, 1100)
        Assert.assertEquals(GS1Code("3112000124").length, 1240)
        Assert.assertEquals(GS1Code("3113001001").length, 1001)

        Assert.assertNull(GS1Code("3102000124").getLength(Unit.LITER))
        Assert.assertNull(GS1Code("3902000124").getLength(Unit.METER))
    }

    @Test
    fun testLiters() {
        // net volume in l, no decimal digits: 1l
        val code1 = GS1Code("3150000001")
        val liters1 = code1.getLiters(Unit.LITER)
        Assert.assertEquals(liters1, BigDecimal("1"))

        // net volume in l, no decimal digits: 1l
        val code2 = GS1Code("3150000001")
        val liters2 = code2.getLiters(Unit.MILLILITER)
        Assert.assertEquals(liters2, BigDecimal("1000"))

        // net volume in l, 3 decimal digits: 1.02l
        val code3 = GS1Code("3153001020")
        val liters3 = code3.getLiters(Unit.MILLILITER)
        Assert.assertEquals(liters3, BigDecimal("1020"))

        // net volume in l, 2 decimal digits: 1.23l
        val code4 = GS1Code("3152000123")
        val liters4 = code4.getLiters(Unit.CENTILITER)
        Assert.assertEquals(liters4, BigDecimal("123"))

        // net volume in l, 2 decimal digits: 1.24l
        val code5 = GS1Code("3152000124")
        val liters5 = code5.getLiters(Unit.DECILITER)
        Assert.assertEquals(liters5, BigDecimal("12.4"))

        Assert.assertEquals(GS1Code("3150000001").liters, 1000)
        Assert.assertEquals(GS1Code("3151000011").liters, 1100)
        Assert.assertEquals(GS1Code("3152000124").liters, 1240)
        Assert.assertEquals(GS1Code("3153001001").liters, 1001)

        Assert.assertNull(GS1Code("3102000124").getLength(Unit.SQUARE_CENTIMETER))
        Assert.assertNull(GS1Code("3902000124").getLiters(Unit.LITER))
    }

    @Test
    fun testVolume() {
        // net volume in m^3, no decimal digits: 1m^3
        val code1 = GS1Code("3160000001")
        val volume1 = code1.getVolume(Unit.CUBIC_METER)
        Assert.assertEquals(volume1, BigDecimal("1"))

        // net volume in m^3, no decimal digits: 1m^3
        val code2 = GS1Code("3160000001")
        val volume2 = code2.getVolume(Unit.CUBIC_CENTIMETER)
        Assert.assertEquals(volume2, BigDecimal("1000000"))

        // net volume in m^3, 3 decimal digits: 1.02m^3
        val code3 = GS1Code("3163001020")
        val volume3 = code3.getVolume(Unit.CUBIC_METER)
        Assert.assertEquals(volume3, BigDecimal("1.02"))

        // net length in m^3, 3 decimal digits: 1.23m^3
        val code4 = GS1Code("3163001230")
        val volume4 = code4.getVolume(Unit.CUBIC_METER)
        Assert.assertEquals(volume4, BigDecimal("1.23"))

        // net length in m^3, 2 decimal digits: 1.24m^3
        val code5 = GS1Code("3162000124")
        val volume5 = code5.getVolume(Unit.CUBIC_CENTIMETER)
        Assert.assertEquals(volume5, BigDecimal("1240000"))

        Assert.assertEquals(GS1Code("3160000001").volume, 1000000)
        Assert.assertEquals(GS1Code("3161000011").volume, 1100000)
        Assert.assertEquals(GS1Code("3162000124").volume, 1240000)
        Assert.assertEquals(GS1Code("3163001001").volume, 1001000)

        Assert.assertNull(GS1Code("3102000124").getVolume(Unit.LITER))
        Assert.assertNull(GS1Code("3902000124").getVolume(Unit.CUBIC_CENTIMETER))
    }

    @Test
    fun testArea() {
        // net area in m^2, no decimal digits: 1m^2
        val code1 = GS1Code("3140000001")
        val area1 = code1.getArea(Unit.SQUARE_METER)
        Assert.assertEquals(area1, BigDecimal(1))

        // net area in m^2, no decimal digits: 1m^2
        val code2 = GS1Code("3140000001")
        val area2 = code2.getArea(Unit.SQUARE_CENTIMETER)
        Assert.assertEquals(area2, BigDecimal(10000))

        // net area in m^2, 3 decimal digits: 1.02m^2
        val code3 = GS1Code("3143001020")
        val area3 = code3.getArea(Unit.SQUARE_CENTIMETER)
        Assert.assertEquals(area3, BigDecimal(10200))

        // net area in m^2, 3 decimal digits: 1.23m^2
        val code4 = GS1Code("3143001230")
        val area4 = code4.getArea(Unit.SQUARE_CENTIMETER)
        Assert.assertEquals(area4, BigDecimal(12300))

        // net area in m^2, 2 decimal digits: 1.24m^2
        val code5 = GS1Code("3142000124")
        val area5 = code5.getArea(Unit.SQUARE_DECIMETER)
        Assert.assertEquals(area5, BigDecimal(124))

        Assert.assertEquals(GS1Code("3140000001").area, 10000)
        Assert.assertEquals(GS1Code("3141000011").area, 11000)
        Assert.assertEquals(GS1Code("3142000124").area, 12400)
        Assert.assertEquals(GS1Code("3143001001").area, 10010)

        Assert.assertNull(GS1Code("3102000124").getArea(Unit.LITER))
        Assert.assertNull(GS1Code("3902000124").getArea(Unit.SQUARE_CENTIMETER))
    }

    @Test
    fun testRawPrice() {
        // amount payable, no decimal digits: 12
        val code1 = GS1Code("392012")
        val price1 = code1.price
        Assert.assertEquals(price1?.price, BigDecimal("12"))
        Assert.assertEquals(price1?.currencyCode, null)

        // amount payable, 2 decimal digits: 12.34
        val code2 = GS1Code("39221234")
        val price2 = code2.price
        Assert.assertEquals(price2?.price, BigDecimal("12.34"))
        Assert.assertEquals(price2?.currencyCode, null)

        // amount payable in EUR (978), no decimal digits: 13
        val code3 = GS1Code("393097813")
        val price3 = code3.price
        Assert.assertEquals(price3?.price, BigDecimal("13"))
        Assert.assertEquals(price3?.currencyCode, "978")

        // amount payable in EUR (978), 2 decimal digits: 13.45
        val code4 = GS1Code("39329781345")
        val price4 = code4.price
        Assert.assertEquals(price4?.price, BigDecimal("13.45"))
        Assert.assertEquals(price4?.currencyCode, "978")
    }

    @Test
    fun testAmount() {
        val code1 = GS1Code("3042")
        Assert.assertEquals(code1.amount, 42)

        val code2 = GS1Code("30xx")
        Assert.assertEquals(code2.amount, null)
    }

    @Test
    fun testRetailerCodes() {
        // missing a (GS) after AI 30, and has invalid chars in AI 10
        val code = GS1Code("010000000001234530000000011520101510              CHARGE")
        Assert.assertEquals(code.gtin, "00000000012345")
        Assert.assertEquals(code.amount, 1) // works because the regex is greedy and
        Assert.assertEquals(code.elements[2].identifier.prefix, "15")
        Assert.assertEquals(code.elements[2].values[0], "201015")
        Assert.assertEquals(code.skipped, listOf("              CHARGE"))

        // standard conforming version of the same code
        val code2 = GS1Code("0100000000012345301${GS}1520101510CHARGE")
        Assert.assertEquals(code2.gtin, "00000000012345")
        Assert.assertEquals(code2.amount, 1)
        Assert.assertEquals(code2.elements[2].identifier.prefix, "15")
        Assert.assertEquals(code2.elements[2].values[0], "201015")
        Assert.assertEquals(code2.skipped.size, 0)
    }

    private fun check(code: String, data: Map<String, String>? = null, skipped: List<String>? = null) {
        val gs1Code = GS1Code(code)
        var validCount = 0
        data?.forEach {
            gs1Code.elements.forEach { it2 ->
                if (it.key == it2.identifier.prefix) {
                    Assert.assertEquals(it.value, it2.values[0])
                    validCount++
                }
            }
        }

        Assert.assertTrue(validCount == (data?.size ?: 0))
        Assert.assertTrue(skipped == null || gs1Code.skipped == skipped)
    }
}