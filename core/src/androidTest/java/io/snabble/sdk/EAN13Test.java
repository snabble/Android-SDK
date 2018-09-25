package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.ScannableCode;

public class EAN13Test extends SnabbleSdkTest {
    @Test
    public void testEAN13() {
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001801").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001802").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001803").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001804").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001805").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001806").getClass());
        Assert.assertEquals(EAN13.class, ScannableCode.parse(project, "4029764001807").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001808").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001809").getClass());
        Assert.assertEquals(ScannableCode.class, ScannableCode.parse(project, "4029764001800").getClass());

        Assert.assertEquals(7, EAN13.checksum("402976400180"));
        Assert.assertEquals(0, EAN13.checksum("200123400000"));
        Assert.assertEquals(7, EAN13.checksum("200123500129"));
        Assert.assertEquals(3, EAN13.checksum("629104150021"));
        Assert.assertEquals(2, EAN13.checksum("222111100000"));
        Assert.assertEquals(1, EAN13.checksum("222111200000"));
    }

    @Test
    public void testInternalChecksum() {
        checkInternalChecksum(6, "2810276146856");
        checkInternalChecksum(5, "2810275005024");
        checkInternalChecksum(4, "2746724008288");
        checkInternalChecksum(0, "2850090007827");
        checkInternalChecksum(6, "2957776004085");
        checkInternalChecksum(2, "2957822002843");
        checkInternalChecksum(6, "2810606013360");
        checkInternalChecksum(0, "2810270002387");
        checkInternalChecksum(8, "2810478001908");
        checkInternalChecksum(2, "2893482001744");
        checkInternalChecksum(6, "2957796002801");
        checkInternalChecksum(5, "2893035003607");
        checkInternalChecksum(5, "2810115003609");
        checkInternalChecksum(4, "2810444001505");
        checkInternalChecksum(5, "2893525001724");
        checkInternalChecksum(3, "2957783000742");
        checkInternalChecksum(0, "2958300015089");
        checkInternalChecksum(7, "2810307003349");
        checkInternalChecksum(9, "2810029003221");
        checkInternalChecksum(0, "2810030004729");
        checkInternalChecksum(5, "2956755020146");
        checkInternalChecksum(6, "2957736007385");
        checkInternalChecksum(9, "2957679012040");
        checkInternalChecksum(3, "2810063024800");
    }

    private void checkInternalChecksum(int checksum, String code) {
        Assert.assertEquals(checksum, EAN13.internalChecksum(code));
    }
}
