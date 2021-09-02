package io.snabble.sdk.utils.security;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;

import androidx.annotation.RequiresApi;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;

@RequiresApi(api = Build.VERSION_CODES.M)
public class KeyStoreCipherMarshmallow extends KeyStoreCipher {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final byte[] FIXED_IV = new byte[] { 30, 119, 28, 107, 29, -26, 62, 115, 40, 123, 35, 114, -75, -116, -41, 33 };

    private KeyStore keyStore;
    private String alias;
    private boolean requireUserAuthentication;

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
            if (!isKeyAccessible()) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);

                spec.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
                spec.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
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

                Logger.d("New key generated for alias %s", alias);
                Logger.d("KeyStore id = %s", id());
            }
        } catch (Exception e) {
            Logger.d("Could not generate key %s", e.getMessage());
            return false;
        }

        return true;
    }

    private boolean isKeyAccessible() {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(FIXED_IV);
            Key key = keyStore.getKey(alias, null);
            c.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
            return true;
        } catch (UserNotAuthenticatedException e) {
            return true;
        } catch (Exception e) {
            Logger.errorEvent("KeyStore inaccessible: " + e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public String id() {
        try {
            Date date = keyStore.getCreationDate(alias);
            if (date == null) {
                return null;
            }

            return Utils.sha1Hex(Long.toString(date.getTime()));
        } catch (KeyStoreException ignored) { }

        return null;
    }

    @Override
    public void validate() {
        createKeys();
    }

    public void purge() {
        try {
            keyStore.deleteEntry(alias);
        } catch (Exception e) {
            Logger.e("Could not purge key store: %s", e.getMessage());
        }

        createKeys();
    }

    public byte[] encrypt(byte[] input) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(FIXED_IV);
            c.init(Cipher.ENCRYPT_MODE, keyStore.getKey(alias, null), ivParameterSpec);
            return c.doFinal(input);
        } catch (KeyPermanentlyInvalidatedException e) {
            Logger.e("Key permanently invalidated");
        } catch (Exception e) {
            Logger.e("Could not encrypt data: %s", e.getMessage());
        }

        return null;
    }

    public byte[] decrypt(byte[] encrypted) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(FIXED_IV);
            c.init(Cipher.DECRYPT_MODE, keyStore.getKey(alias, null), ivParameterSpec);
            return c.doFinal(encrypted);
        } catch (KeyPermanentlyInvalidatedException e) {
            Logger.e("Key permanently invalidated");
        } catch (Exception e) {
            Logger.e("Could not decrypt data: %s", e.getMessage());
        }

        return null;
    }
}
