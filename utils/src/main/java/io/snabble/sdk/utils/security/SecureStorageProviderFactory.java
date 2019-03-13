package io.snabble.sdk.utils.security;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import io.snabble.sdk.utils.Logger;

public class SecureStorageProviderFactory {
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static SecureStorageProvider create(Context context, String tag, boolean requireUserAuthentication) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new SecureStorageProviderMarshmallow(tag, requireUserAuthentication);
            } else {
                return new SecureStorageProviderJellyBeanMR2(context.getApplicationContext(), tag, requireUserAuthentication);
            }
        } catch (Exception e) {
            Logger.e("Could not create SecureStorageProvider: %s", e.getMessage());
        }

        return null;
    }
}
