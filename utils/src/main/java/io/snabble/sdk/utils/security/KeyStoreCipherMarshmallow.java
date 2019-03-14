package io.snabble.sdk.utils.security;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import androidx.annotation.RequiresApi;
import io.snabble.sdk.utils.Logger;

@RequiresApi(api = Build.VERSION_CODES.M)
public class KeyStoreCipherMarshmallow extends KeyStoreCipher {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final byte[] FIXED_IV = new byte[] { 30, 119, 28, 107, 29, -26, 62, 115, 40, 123, 35, 114 };

    private final KeyStore keyStore;
    private final String alias;
    private final boolean requireUserAuthentication;

    KeyStoreCipherMarshmallow(String alias, boolean requireUserAuthentication) {
        this.alias = alias + "_M";
        this.requireUserAuthentication = requireUserAuthentication;

        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            if (!createKeys()) {
                throw new IllegalStateException("Could not create key pairs");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize AndroidKeyStore: " + e.getMessage());
        }
    }

    private boolean createKeys() {
        try {
            if (!keyStore.containsAlias(alias)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);

                spec.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
                spec.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
                spec.setRandomizedEncryptionRequired(false);

                if (requireUserAuthentication) {
                    spec.setUserAuthenticationRequired(true);
                    spec.setUserAuthenticationValidityDurationSeconds(30);
                }

                // Allow the user to add new fingerprints and keep the key pair, only works for Android 7+
                // For Android 6 the keys are unfortunately invalidated when new fingerprints are added
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    spec.setInvalidatedByBiometricEnrollment(false);
                }

                keyGenerator.init(spec.build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public int size() {
        try {
            return keyStore.size();
        } catch (KeyStoreException e) {
            return 0;
        }
    }

    public void invalidate() {
        try {
            keyStore.deleteEntry(alias);
            createKeys();
        } catch (Exception e) {
            Logger.e("Could not invalidate key store: %s", e.getMessage());
        }
    }

    public byte[] encrypt(byte[] input) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.ENCRYPT_MODE, keyStore.getKey(alias, null), new GCMParameterSpec(128, FIXED_IV));
            return c.doFinal(input);
        } catch (Exception e) {
            Logger.e("Could not encrypt data: %s", e.getMessage());
        }

        return null;
    }

    public byte[] decrypt(byte[] encrypted) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.DECRYPT_MODE, keyStore.getKey(alias, null), new GCMParameterSpec(128, FIXED_IV));
            return c.doFinal(encrypted);
        } catch (Exception e) {
            Logger.e("Could not decrypt data: %s", e.getMessage());
        }

        return null;
    }
}
