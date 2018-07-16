package io.snabble.sdk.codes;

import android.util.SparseIntArray;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;

import io.snabble.sdk.SnabbleSdk;

public class EAN13 extends ScannableCode implements Serializable {
    private final static String[] germanPrintPrefixes = new String[] {
            "414", "419", "434", "449"
    };

    private String lookupCode;
    private int embeddedData;
    private boolean hasUnitData;
    private boolean hasPriceData;
    private boolean hasWeighData;

    private boolean verifyInternalEanChecksum;
    private boolean useGermanPrintPrefix;

    EAN13(String code, SnabbleSdk snabbleSdk) {
        super(code);

        if(!isEan13(code)){
            throw new IllegalArgumentException("Not a valid EAN13 code");
        }

        verifyInternalEanChecksum = snabbleSdk.isVerifyingInternalEanChecksum();
        useGermanPrintPrefix = snabbleSdk.isUsingGermanPrintPrefix();

        for (String prefix : snabbleSdk.getWeighPrefixes()) {
            if (code.startsWith(prefix)) {
                hasWeighData = true;
            }
        }

        for (String prefix : snabbleSdk.getPricePrefixes()) {
            if (code.startsWith(prefix)) {
                hasPriceData = true;
            }
        }

        for (String prefix : snabbleSdk.getUnitPrefixes()) {
            if (code.startsWith(prefix)) {
                hasUnitData = true;
            }
        }

        if(containsGermanPrintPrefix()) {
            lookupCode = code.substring(0, 3) + "0000000000";
            embeddedData = Integer.parseInt(code.substring(8, 12));
            hasPriceData = true;
        } else if(isEmbeddedDataOk()) {
            lookupCode = code.substring(0, 6) + "0000000";
            embeddedData = Integer.parseInt(code.substring(7, 12));

            if (hasUnitData && embeddedData == 0) {
                embeddedData = 1;

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(code.substring(0, code.length() - 2));
                stringBuilder.append("1");
                stringBuilder.replace(6, 7, String.valueOf(internalChecksum(stringBuilder.toString())));
                stringBuilder.append(String.valueOf(checksum(stringBuilder.toString())));

                this.code = stringBuilder.toString();
            }
        } else {
            lookupCode = code;
            hasWeighData = false;
            hasUnitData = false;
            hasPriceData = false;
        }
    }

    private boolean containsGermanPrintPrefix() {
        if(!useGermanPrintPrefix){
            return false;
        }

        return ArrayUtils.contains(germanPrintPrefixes, getCode().substring(0, 3));
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
        return hasUnitData;
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
        return hasUnitData || hasPriceData || hasWeighData;
    }

    @Override
    public boolean isEmbeddedDataOk() {
        if(containsGermanPrintPrefix()){
            return true;
        }

        if(!hasEmbeddedData()){
            return false;
        }

        if(!verifyInternalEanChecksum){
            return true;
        }

        String code = getCode();
        int digit = Character.digit(code.charAt(6), 10);
        int check = internalChecksum(code);
        return check == digit;
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
            return d == checksum(ean.substring(0, ean.length() - 1));
        }

        return false;
    }

    public static int internalChecksum(String code) {
        int sum = 0;
        for (int i = 0; i < 5; i++) {
            int d = Character.digit(code.charAt(i + 7), 10);
            sum += i + weightedProduct(i, d);
        }
        int mod10 = (10 - (sum % 10)) % 10;
        return check5minusReverse.get(mod10, -1);
    }

    private static int weightedProduct(int index, int digit){
        switch(index){
            case 0:
            case 3:
                return check5plus.get(digit, -1);
            case 1:
            case 4:
                return check2minus.get(digit, -1);
            case 2:
                return check5minus.get(digit, -1);
            default:
                return -1;
        }
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
        return ean != null && ean.length() == 13 && allDigits(ean);

    }

    public static boolean isEan13(String ean) {
        return isValidString(ean) && checkChecksum(ean);

    }

    private static SparseIntArray check5plus = new SparseIntArray();
    private static SparseIntArray check2minus = new SparseIntArray();
    private static SparseIntArray check5minus = new SparseIntArray();
    private static SparseIntArray check5minusReverse = new SparseIntArray();

    static {
        check5plus.put(0, 0);
        check5plus.put(1, 5);
        check5plus.put(2, 1);
        check5plus.put(3, 6);
        check5plus.put(4, 2);
        check5plus.put(5, 7);
        check5plus.put(6, 3);
        check5plus.put(7, 8);
        check5plus.put(8, 4);
        check5plus.put(9, 9);

        check2minus.put(0, 0);
        check2minus.put(1, 2);
        check2minus.put(2, 4);
        check2minus.put(3, 6);
        check2minus.put(4, 8);
        check2minus.put(5, 9);
        check2minus.put(6, 1);
        check2minus.put(7, 3);
        check2minus.put(8, 5);
        check2minus.put(9, 7);

        check5minus.put(0, 0);
        check5minus.put(1, 5);
        check5minus.put(2, 9);
        check5minus.put(3, 4);
        check5minus.put(4, 8);
        check5minus.put(5, 3);
        check5minus.put(6, 7);
        check5minus.put(7, 2);
        check5minus.put(8, 6);
        check5minus.put(9, 1);

        for(int i=0; i<check5minus.size(); i++){
            check5minusReverse.put(check5minus.valueAt(i), check5minus.keyAt(i));
        }
    }
}
