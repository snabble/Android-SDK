package io.snabble.sdk.payment;

import org.iban4j.IbanUtil;

/**
 * Class for validating IBAN numbers
 */
public class IBAN {
    /**
     * Validate a IBAN for correctness.
     */
    public static boolean validate(String iban) {
        try {
            IbanUtil.validate(iban);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
