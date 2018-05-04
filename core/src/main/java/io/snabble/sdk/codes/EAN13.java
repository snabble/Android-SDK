package io.snabble.sdk.codes;

import java.io.Serializable;

public class EAN13 extends ScannableCode implements Serializable {
    private String lookupCode;
    private int embeddedData;
    private boolean hasAmountData;
    private boolean hasPriceData;
    private boolean hasWeighData;

    public EAN13(String code, String[] weighPrefixes, String[] pricePrefixes, String[] amountPrefixes) {
        super(code);

        if(!isEan13(code)){
            throw new IllegalArgumentException("Not a valid EAN13 code");
        }

        for(String prefix : weighPrefixes){
            if(code.startsWith(prefix)){
                hasWeighData = true;
            }
        }

        for(String prefix : pricePrefixes){
            if(code.startsWith(prefix)){
                hasPriceData = true;
            }
        }

        for(String prefix : amountPrefixes){
            if(code.startsWith(prefix)){
                hasAmountData = true;
            }
        }

        lookupCode = code.substring(0, 6) + "0000000";
        embeddedData = Integer.parseInt(code.substring(7, 12));
    }

    @Override
    public String getLookupCode() {
        return lookupCode;
    }

    @Override
    public int getEmbeddedData() {
        return embeddedData;
    }

    @Override
    public boolean hasAmountData() {
        return hasAmountData;
    }

    @Override
    public boolean hasPriceData() {
        return hasPriceData;
    }

    @Override
    public boolean hasWeighData() {
        return hasWeighData;
    }

    @Override
    public boolean hasEmbeddedData(){
        return hasAmountData || hasPriceData || hasWeighData;
    }

    /**
     * Calculates the checksum of an given ean string.
     * <p>
     * See https://en.wikipedia.org/wiki/International_Article_Number_(EAN)
     *
     * @param ean A string of digits
     * @return returns integer from 0, 9. -1 if the input String is not a string of all digits.
     */
    public static int checksum(String ean) {
        int sum = 0;
        for (int i = 0; i < ean.length(); i++) {
            int d = Character.digit(ean.charAt(i), 10);
            sum += (i % 2) == 0 ? d : d * 3;
        }

        int d = 10 - (sum % 10);
        return d == 10 ? 0 : d;
    }

    public static boolean checkChecksum(String ean) {
        if (isValidString(ean)) {
            int d = Character.digit(ean.charAt(ean.length() - 1), 10);
            if (d == checksum(ean.substring(0, ean.length() - 1))) {
                return true;
            }
        }

        return false;
    }

    private static boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidString(String ean) {
        if (ean != null && ean.length() == 13 && allDigits(ean)) {
            return true;
        }

        return false;
    }

    public static boolean isEan13(String ean) {
        if (isValidString(ean) && checkChecksum(ean)) {
            return true;
        }

        return false;
    }
}
