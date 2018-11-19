package io.snabble.sdk.payment;

import org.iban4j.IbanUtil;

public class IBAN {
    public static boolean validate(String iban) {
        try {
            IbanUtil.validate(iban);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
