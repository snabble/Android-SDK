package io.snabble.sdk.payment;

import org.iban4j.*;

public class SEPAPaymentCredentials extends PaymentCredentials {
    private String obfuscatedIBAN;
    private String encryptedIBAN;

    public SEPAPaymentCredentials(String iban) {
        if(!validateIBAN(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        this.obfuscatedIBAN = obfuscate(iban);

        // TODO encrypt
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

    public static boolean validateIBAN(String iban) {
        try {
            IbanUtil.validate(iban);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
