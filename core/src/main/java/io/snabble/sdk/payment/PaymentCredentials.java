package io.snabble.sdk.payment;

import android.util.Base64;

import org.iban4j.IbanUtil;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.crypto.Cipher;
import io.snabble.sdk.PublicKey;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.GsonHolder;

public class PaymentCredentials {
    public enum Type {
        SEPA
    }

    private static class SepaData {
        private String name;
        private String iban;
    }

    private String obfuscatedIBAN;
    private String encryptedData;
    private Type type;

    private PaymentCredentials() {

    }

    public static PaymentCredentials fromSEPA(String name, String iban) {
        PaymentCredentials pc = new PaymentCredentials();
        pc.type = Type.SEPA;

        List<PublicKey> publicKeys = Snabble.getInstance().getValidPublicKeys();
        if (publicKeys.size() == 0) {
            throw new IllegalStateException("No public keys in metadata");
        }

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid Name");
        }

        if (!IBAN.validate(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        pc.obfuscatedIBAN = pc.obfuscate(iban);

        SepaData data = new SepaData();
        data.name = name;
        data.iban = iban;
        String json = GsonHolder.get().toJson(data, SepaData.class);

        pc.encryptedData = pc.encrypt(publicKeys.get(0), json.getBytes());
        return pc;
    }

    public Type getType() {
        return type;
    }

    private String obfuscate(String s) {
        int numChars = 4;

        StringBuilder sb = new StringBuilder(s.length());
        for(int i=0; i<s.length() - numChars; i++){
            sb.append('*');
        }
        sb.append(s.substring(s.length() - numChars, s.length()));

        return sb.toString();
    }

    private String encrypt(PublicKey publicKey, byte[] data) {
        try {
            byte[] publicBytes = Base64.decode(publicKey.getCipher(), Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            java.security.PublicKey pubKey = keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(data);
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public String getObfuscatedId() {
        return obfuscatedIBAN;
    }

    public String getEncryptedData() {
        return encryptedData;
    }
}
