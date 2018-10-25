package io.snabble.sdk.payment;

import org.iban4j.*;

public class SEPAPaymentCredentials extends PaymentCredentials {
    private String obfuscatedIBAN;
    private String encryptedIBAN;
    private String encryptedName;

    public SEPAPaymentCredentials(String name, String iban) {
        if(name == null || name.length() == 0) {
            throw new IllegalArgumentException("Invalid Name");
        }

        if(!validateIBAN(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        this.obfuscatedIBAN = obfuscate(iban);

        // TODO encrypt
        this.encryptedName = "";
        this.encryptedIBAN = "";
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

    @Override
    public String getObfuscatedId() {
        return obfuscatedIBAN;
    }

    public String getEncryptedIBAN() {
        return encryptedIBAN;
    }

    public String getEncryptedName() {
        return encryptedName;
    }

    public static boolean validateIBAN(String iban) {
        try {
            IbanUtil.validate(iban);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
