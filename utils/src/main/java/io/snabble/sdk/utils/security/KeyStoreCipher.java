package io.snabble.sdk.utils.security;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import io.snabble.sdk.utils.Logger;

public abstract class KeyStoreCipher {
    public abstract String id();
    public abstract void validate();
    public abstract void purge();
    public abstract byte[] encrypt(byte[] data);
    public abstract byte[] decrypt(byte[] encrypted);

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static KeyStoreCipher create(Context context, String tag, boolean requireUserAuthentication) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new KeyStoreCipherMarshmallow(tag, requireUserAuthentication);
            } else {
                return new KeyStoreCipherJellyBeanMR2(context.getApplicationContext(), tag, requireUserAuthentication);
            }
        } catch (Exception e) {
            Logger.e("Could not create KeyStoreCipher: %s", e.getMessage());
        }

        return null;
    }
}
