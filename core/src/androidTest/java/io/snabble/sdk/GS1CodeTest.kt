package io.snabble.sdk

import io.snabble.sdk.codes.gs1.GS1Code
import org.junit.Assert
import org.junit.Test

class GS1CodeTest {
    companion object {
        const val GS = "\u001D"
    }

    @Test
    fun testInvalidCodes() {
        val code1 = GS1Code("")
        Assert.assertEquals(code1.identifiers.size, 0)
        Assert.assertEquals(code1.skipped.size, 0)

        val code2 = GS1Code("${GS}${GS}")
        Assert.assertEquals(code2.identifiers.size, 0)
        Assert.assertEquals(code1.skipped.size, 0) // FIXME: different than in iOS SDK - not really important

        val code3 = GS1Code("asdfghjklöä")
        Assert.assertEquals(code3.identifiers.size, 0)
        Assert.assertEquals(code3.skipped.size, 1)

        val code4 = GS1Code("   0100000000000000    ")
        Assert.assertEquals(code4.identifiers.size, 0)
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
        Assert.assertEquals(code1.identifiers[0].values.size, 2)
        Assert.assertEquals(code1.identifiers[0].values[0], "111")
        Assert.assertEquals(code1.identifiers[0].values[1], "0123456")

        // amount payable, invalid data
        val code2 = GS1Code("3910aaabbbb")
        Assert.assertEquals(code2.identifiers.size, 0)
        Assert.assertEquals(code2.skipped.size, 1)
        Assert.assertEquals(code2.skipped[0], "3910aaabbbb")
    }

    private fun check(code: String, data: Map<String, String>? = null, skipped: List<String>? = null) {
        val gs1Code = GS1Code(code)
        var validCount = 0
        data?.forEach {
            gs1Code.identifiers.forEach { it2 ->
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