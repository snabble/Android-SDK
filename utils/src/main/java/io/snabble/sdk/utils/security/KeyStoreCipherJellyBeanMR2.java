package io.snabble.sdk.utils.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class KeyStoreCipherJellyBeanMR2 extends KeyStoreCipher {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String RSA = "RSA";
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";
    private static final String AES = "AES";
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final byte[] FIXED_IV = new byte[] { 30, 119, 28, 107, 29, -26, 62, 115, 40, 123, 35, 114, -75, -116, -41, 33 };
    private static final String SHARED_PREFERENCES_TAG = "snabble_SecureStorageProviderJellyBeanMR2";

    private KeyStore keyStore;
    private String alias;
    private SharedPreferences sharedPreferences;
    private boolean requireUserAuthentication;
    private Context context;

    KeyStoreCipherJellyBeanMR2(Context context, String alias, boolean requireUserAuthentication) {
        this.alias = alias + "_JB_MR2";
        this.context = context;
        this.requireUserAuthentication = requireUserAuthentication;

        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

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

    @SuppressLint("ApplySharedPref")
    private boolean createKeys() {
        try {
            if (!isKeyAccessible()) {
                keyStore.deleteEntry(alias);
                sharedPreferences.edit().remove(alias).commit();

                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 10);

                KeyPairGeneratorSpec.Builder spec = new KeyPairGeneratorSpec.Builder(context);

                spec.setAlias(alias);
                spec.setSubject(new X500Principal("CN=" + alias));

                if (requireUserAuthentication) {
                    spec.setEncryptionRequired();
                }

                spec.setSerialNumber(BigInteger.TEN);
                spec.setStartDate(start.getTime());
                spec.setEndDate(end.getTime());

                KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA, ANDROID_KEY_STORE);
                kpg.initialize(spec.build());
                kpg.generateKeyPair();
            }

            String encryptedKeyB64 = sharedPreferences.getString(alias, null);
            if (encryptedKeyB64 == null) {
                byte[] key = new byte[16];
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(key);
                byte[] encryptedKey = rsaEncrypt(key);
                encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(alias, encryptedKeyB64);
                edit.apply();
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean isKeyAccessible() {
        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
            Cipher inputCipher = Cipher.getInstance(RSA_MODE);
            inputCipher.init(Cipher.ENCRYPT_MODE, entry.getCertificate().getPublicKey());
            return true;
        } catch (Exception e) {
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

    @SuppressLint("ApplySharedPref")
    public void purge() {
        try {
            keyStore.deleteEntry(alias);
            sharedPreferences.edit().remove(alias).commit();

            createKeys();
        } catch (Exception e) {
            Logger.e("Could not purge key store: %s", e.getMessage());
        }
    }

    private byte[] rsaEncrypt(byte[] secret) throws Exception {
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);
        inputCipher.init(Cipher.ENCRYPT_MODE, entry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        return outputStream.toByteArray();
    }

    private byte[] rsaDecrypt(byte[] encrypted) throws Exception {
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
        Cipher output = Cipher.getInstance(RSA_MODE);
        output.init(Cipher.DECRYPT_MODE, entry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(encrypted), output);

        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i);
        }

        return bytes;
    }

    private Key getSecretKey() throws Exception {
        byte[] encryptedKey = Base64.decode(sharedPreferences.getString(alias, null), Base64.DEFAULT);
        return new SecretKeySpec(rsaDecrypt(encryptedKey), AES);
    }

    public byte[] encrypt(byte[] input) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(FIXED_IV);
            c.init(Cipher.ENCRYPT_MODE, getSecretKey(), ivParameterSpec);
            return c.doFinal(input);
        } catch (Exception e) {
            Logger.e("Could not encrypt data: %s", e.getMessage());
        }

        return null;
    }

    public byte[] decrypt(byte[] encrypted) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(FIXED_IV);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(), ivParameterSpec);
            return c.doFinal(encrypted);
        } catch (Exception e) {
            Logger.e("Could not decrypt data: %s", e.getMessage());
        }

        return null;
    }
}
