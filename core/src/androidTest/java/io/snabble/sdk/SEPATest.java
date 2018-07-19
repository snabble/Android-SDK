package io.snabble.sdk;


import io.snabble.sdk.payment.SEPAPaymentCredentials;
import org.junit.Assert;
import org.junit.Test;

public class SEPATest extends SnabbleSdkTest {
    @Test
    public void testBICValidation() {
        testBIC("VBRSDE33330");
        testBIC("DGPBDE3MSAL");
        testBIC("INGDDEFFXXX");
        testBIC("SMHBDEFFMUN");
        testBIC("MSFFDEFPXXX");
        testBIC("FODBDE77XXX");
        testBIC("DGPBDE3MDEL");
        testBIC("VBMHDE5FXXX");
        testBIC("DCAGDE6SXXX");
        testBIC("BHFBDEFFSUT");
        testBIC("BYLADEMMXXX");
        testBIC("UGBIDEDDXXX");
        testBIC("COLSDE33XXX");
        testBIC("VBRSDE33341");
        testBIC("ZGIEDEFFXXX");
        testBIC("SPESDE3EXXX");
        testBIC("DAKVDEFFAS2");
        testBIC("SLBSDEFPXXX");
        testBIC("EEXCDE8LXXX");
        testBIC("BLFLDEFFXXX");

        testInvalidBIC("ASDF");
        testInvalidBIC("1SDJFGJ1411");
        testInvalidBIC("AAAAAA01AAA");
        testInvalidBIC("AAAAAA11AAA");
        testInvalidBIC("AAAAAA3OAAA");
        testInvalidBIC("AAAAAAD1XAA");
    }

    private void testInvalidBIC(String bic){
        Assert.assertEquals(false, SEPAPaymentCredentials.validateBIC(bic));
    }

    private void testBIC(String bic){
        Assert.assertEquals(true, SEPAPaymentCredentials.validateBIC(bic));
    }
}


