package io.snabble.sdk.utils;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import io.snabble.sdk.utils.security.KeyStoreCipher;

@RunWith(AndroidJUnit4.class)
public class KeyStoreCipherTest {
    @Test
    public void testEncryptDecrypt() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        KeyStoreCipher keyStoreCipher = KeyStoreCipher.create(context, "test", false);
        Assert.assertNotNull(keyStoreCipher);

        keyStoreCipher.invalidate();

        byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] encrypted = keyStoreCipher.encrypt(data);
        Assert.assertNotNull(encrypted);
        Assert.assertFalse(Arrays.equals(data, encrypted));

        Assert.assertArrayEquals(keyStoreCipher.decrypt(encrypted), data);
    }
}
