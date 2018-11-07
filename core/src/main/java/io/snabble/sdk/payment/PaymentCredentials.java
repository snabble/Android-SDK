package io.snabble.sdk.payment;

import android.util.Base64;

import java.io.InputStream;
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

        pc.obfuscatedIBAN = pc.obfuscate(iban);

        SepaData data = new SepaData();
        data.name = name;
        data.iban = iban;
        String json = GsonHolder.get().toJson(data, SepaData.class);

        X509Certificate certificate = certificates.get(0);
        pc.encryptedData = pc.encrypt(certificate, json.getBytes());
        pc.signature = pc.sha256Signature(certificate);

        if(pc.encryptedData == null) {
            return null;
        }

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

    private boolean validateCertificate(X509Certificate certificate) {
        try {
            InputStream is = Snabble.getInstance().getApplication().getResources().openRawResource(R.raw.ca);
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

    private String encrypt(X509Certificate certificate, byte[] data) {
        try {
            if(validateCertificate(certificate)) {
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
        return obfuscatedIBAN;
    }

    public String getEncryptedData() {
        return encryptedData;
    }
}
