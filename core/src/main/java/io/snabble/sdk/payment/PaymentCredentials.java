package io.snabble.sdk.payment;

import android.util.Base64;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import io.snabble.sdk.R;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;
import io.snabble.sdk.utils.security.KeyStoreCipher;

public class PaymentCredentials {
    public enum Type {
        SEPA
    }

    private static class SepaData {
        private String name;
        private String iban;
    }

    private String obfuscatedId;
    private boolean isKeyStoreEncrypted;
    private String encryptedData;
    private String signature;

    private Type type;

    private PaymentCredentials() {

    }

    public static PaymentCredentials fromSEPA(String name, String iban) {
        PaymentCredentials pc = new PaymentCredentials();
        pc.type = Type.SEPA;

        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        if (certificates.size() == 0) {
            return null;
        }

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid Name");
        }

        if (!IBAN.validate(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        pc.obfuscatedId = pc.obfuscate(iban);

        SepaData data = new SepaData();
        data.name = name;
        data.iban = iban;
        String json = GsonHolder.get().toJson(data, SepaData.class);

        X509Certificate certificate = certificates.get(0);
        pc.encryptedData = pc.rsaEncrypt(certificate, json.getBytes());
        pc.encrypt();
        pc.signature = pc.sha256Signature(certificate);

        if (pc.encryptedData == null) {
            return null;
        }

        return pc;
    }

    public Type getType() {
        return type;
    }

    private String obfuscate(String s) {
        int numCharsStart = 4;
        int numCharsEnd = 2;

        StringBuilder sb = new StringBuilder(s.length());
        sb.append(s.substring(0, numCharsStart));
        for (int i = numCharsStart; i < s.length() - numCharsEnd; i++) {
            sb.append('\u2022'); // Bullet
        }
        sb.append(s.substring(s.length() - numCharsEnd));

        if (sb.toString().startsWith("DE")) {
            for (int i=4; i<sb.length(); i+=4) {
                sb.insert(i, ' ');
                i++;
            }
        }

        return sb.toString();
    }

    /** Asynchronous encryption using the certificate the backend provided for us **/
    private String rsaEncrypt(X509Certificate certificate, byte[] data) {
        try {
            if (validateCertificate(certificate)) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
                AlgorithmParameterSpec params = new OAEPParameterSpec("SHA-256", "MGF1",
                        new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
                cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey(), params);
                byte[] encrypted = cipher.doFinal(data);
                return Base64.encodeToString(encrypted, Base64.NO_WRAP);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    /** Synchronous encryption using the user credentials **/
    private void encrypt() {
        PaymentCredentialsStore store = Snabble.getInstance().getPaymentCredentialsStore();

        if (store.keyStoreCipher == null) {
            return;
        }

        byte[] keyStoreEncrypted = store.keyStoreCipher.encrypt(encryptedData.getBytes());

        if (keyStoreEncrypted != null) {
            encryptedData = Base64.encodeToString(keyStoreEncrypted, Base64.NO_WRAP);
            isKeyStoreEncrypted = true;
        } else {
            Logger.e("Could not encrypt payment credentials: KeyStore unavailable");
        }
    }

    /** Synchronous decryption using the user credentials **/
    private String decrypt() {
        PaymentCredentialsStore store = Snabble.getInstance().getPaymentCredentialsStore();

        if (store.keyStoreCipher == null) {
            if (isKeyStoreEncrypted) {
                Logger.e("Accessing encrypted data without providing KeyStoreCipher");
            }

            return encryptedData;
        }

        if (isKeyStoreEncrypted) {
            byte[] decrypted = store.keyStoreCipher.decrypt(Base64.decode(encryptedData, Base64.NO_WRAP));
            if (decrypted != null) {
                return new String(decrypted);
            } else {
                Logger.e("Could not decrypt using KeyStoreCipher");
                return null;
            }
        }

        return encryptedData;
    }

    private boolean validateCertificate(X509Certificate certificate) {
        try {
            Snabble snabble = Snabble.getInstance();
            int caResId;

            switch (snabble.getEnvironment()) {
                case PRODUCTION:
                    caResId = R.raw.ca_prod;
                    break;
                case STAGING:
                    caResId = R.raw.ca_staging;
                    break;
                case TESTING:
                    caResId = R.raw.ca_testing;
                    break;
                default:
                    return false;
            }

            InputStream is = Snabble.getInstance().getApplication().getResources().openRawResource(caResId);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate trustedCA = (X509Certificate) certificateFactory.generateCertificate(is);

            List<Certificate> certificates = new ArrayList<>();
            certificates.add(certificate);

            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
            CertPath cp = certificateFactory.generateCertPath(certificates);
            TrustAnchor trustAnchor = new TrustAnchor(trustedCA, null);
            HashSet<TrustAnchor> set = new HashSet<>();
            set.add(trustAnchor);
            PKIXParameters pkixParameters = new PKIXParameters(set);
            pkixParameters.setRevocationEnabled(false);

            try {
                certPathValidator.validate(cp, pkixParameters);
                return true;
            } catch (CertPathValidatorException e) {
                Logger.d("Could not verify certificate: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String sha256Signature(X509Certificate certificate) {
        return Utils.sha256Hex(Utils.hexString(certificate.getSignature()));
    }

    public boolean validate() {
        List<X509Certificate> certificates = Snabble.getInstance().getPaymentSigningCertificates();
        for (X509Certificate cert : certificates) {
            if (sha256Signature(cert).equals(signature)) {
                return true;
            }
        }

        return false;
    }

    public String getObfuscatedId() {
        return obfuscatedId;
    }

    public String getEncryptedData() {
        return decrypt();
    }
}
