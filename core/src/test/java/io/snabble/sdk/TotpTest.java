package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import io.snabble.sdk.auth.Base32String;
import io.snabble.sdk.auth.Totp;

public class TotpTest {
    @Test
    public void testTOTP() throws Base32String.DecodingException {
        String secretString = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA====";
        byte[] secretData = Base32String.decode(secretString);
        Totp totp = new Totp("HmacSHA256", secretData, 8, 30);

        Assert.assertEquals("46119246", totp.generate(59));
        Assert.assertEquals("68084774", totp.generate(1111111109));
        Assert.assertEquals("67062674", totp.generate(1111111111));
        Assert.assertEquals("91819424", totp.generate(1234567890));
        Assert.assertEquals("90698825", totp.generate(2000000000));
        Assert.assertEquals("77737706", totp.generate(20000000000L));
    }
}
