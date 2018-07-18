package io.snabble.sdk.payment;

import org.iban4j.*;

public class SEPACard extends UserPaymentMethod {
    private String ownerName;
    private String obfuscatedIBAN;

    private String encryptedIBAN;
    private String encryptedBIC;

    public SEPACard(String ownerName, String iban, String bic) {
        if(ownerName == null || ownerName.length() == 0) {
            throw new IllegalArgumentException("Invalid ownerName");
        }

        if(!validateIBAN(iban)) {
            throw new IllegalArgumentException("Invalid IBAN");
        }

        if(!validateBIC(bic)) {
            throw new IllegalArgumentException("Invalid BIC");
        }

        this.ownerName = ownerName;
        this.obfuscatedIBAN = obfuscateIBAN(iban);

        // TODO encrypt
        this.encryptedIBAN = "";
        this.encryptedBIC = "";
    }

    private String obfuscateIBAN(String iban) {
        int numChars = 4;

        StringBuilder sb = new StringBuilder(iban.length());
        for(int i=0; i<iban.length() - numChars; i++){
            sb.append('*');
        }
        sb.append(iban.substring(iban.length() - numChars, iban.length()));

        return sb.toString();
    }

    public String getObfuscatedIBAN() {
        return obfuscatedIBAN;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getEncryptedIBAN() {
        return encryptedIBAN;
    }

    public String getEncryptedBIC() {
        return encryptedBIC;
    }

    public static boolean validateBIC(String bic) {
        try {
            BicUtil.validate(bic);
            return true;
        } catch (Exception e) {
            return false;
        }
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
