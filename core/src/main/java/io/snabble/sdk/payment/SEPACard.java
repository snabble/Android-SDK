package io.snabble.sdk.payment;

import org.iban4j.*;

public class SEPACard  {
    private String ownerName;
    private String iban;
    private String bic;

    public SEPACard(String ownerName, String iban, String bic) {
        this.ownerName = ownerName;
        this.iban = iban;
        this.bic = bic;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getIban() {
        return iban;
    }

    public String getBic() {
        return bic;
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
