package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import io.snabble.sdk.codes.EAN8;

public class EAN8Test {
    @Test
    public void testEAN8() {
        Assert.assertTrue(EAN8.isEan8("76543210"));
        Assert.assertTrue(EAN8.isEan8("76543210"));
        Assert.assertFalse(EAN8.isEan8("76543211"));
        Assert.assertFalse(EAN8.isEan8("76543212"));
        Assert.assertFalse(EAN8.isEan8("76543213"));
        Assert.assertFalse(EAN8.isEan8("76543214"));
        Assert.assertFalse(EAN8.isEan8("76543215"));
        Assert.assertFalse(EAN8.isEan8("76543216"));
        Assert.assertFalse(EAN8.isEan8("76543217"));
        Assert.assertFalse(EAN8.isEan8("76543218"));
        Assert.assertFalse(EAN8.isEan8("76543219"));

        Assert.assertFalse(EAN8.isEan8("87654320"));
        Assert.assertFalse(EAN8.isEan8("87654321"));
        Assert.assertFalse(EAN8.isEan8("87654322"));
        Assert.assertFalse(EAN8.isEan8("87654323"));
        Assert.assertFalse(EAN8.isEan8("87654324"));
        Assert.assertTrue(EAN8.isEan8("87654325"));
        Assert.assertFalse(EAN8.isEan8("87654326"));
        Assert.assertFalse(EAN8.isEan8("87654327"));
        Assert.assertFalse(EAN8.isEan8("87654328"));
        Assert.assertFalse(EAN8.isEan8("87654329"));

        Assert.assertEquals(0, EAN8.checksum("1234567"));
        Assert.assertEquals(6, EAN8.checksum("6234713"));
        Assert.assertEquals(2, EAN8.checksum("2347141"));
        Assert.assertEquals(7, EAN8.checksum("8565713"));

        Assert.assertEquals(5, EAN8.checksum("87654325"));
        Assert.assertEquals(5, EAN8.checksum("8765432"));

        Assert.assertEquals(7, EAN8.checksum("90311017"));
        Assert.assertEquals(7, EAN8.checksum("9031101"));

        Assert.assertFalse(EAN8.isEan8(""));
        Assert.assertFalse(EAN8.isEan8("123456"));
        Assert.assertFalse(EAN8.isEan8("123456789"));
        Assert.assertFalse(EAN8.isEan8("1234567A"));
    }
}
