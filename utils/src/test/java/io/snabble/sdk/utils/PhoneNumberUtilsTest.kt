package io.snabble.sdk.utils

import io.snabble.sdk.utils.PhoneNumberUtils.convertToTelSchema
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test
    fun validateParsing() {
        assertEquals("tel:+4912345678900", convertToTelSchema("+49 123 456789-00"))
        assertEquals("tel:+4915781234567", convertToTelSchema("+49 1578 1234567 "))
        assertEquals("tel:+4912345678", convertToTelSchema("+49 (0) 123 45678\n"))
        assertEquals("tel:02111234565", convertToTelSchema("0211/\t123456-5"))
        assertEquals("tel:+12345645690", convertToTelSchema("+123(0)456/456-90"))
        assertEquals("tel:+12345645690", convertToTelSchema("+123\t(0)456 / 456-9(0)"))
    }
}