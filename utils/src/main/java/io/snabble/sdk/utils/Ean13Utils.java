package io.snabble.sdk.utils;

public class Ean13Utils {
    /**
     * Calculates the checksum of an given ean string.
     * <p>
     * See https://en.wikipedia.org/wiki/International_Article_Number_(EAN)
     *
     * @param ean A string of digits
     * @return returns integer from 0, 9. -1 if the input String is not a string of all digits.
     */
    public static int checksum(String ean) {
        if (ean == null || !allDigits(ean)) {
            return -1;
        }

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

    public static String toWeighItemId(String ean) {
        if (isEan13(ean)) {
            return ean.substring(0, 6) + "0000000";
        }

        return null;
    }

    public static int gramsOfEan(String ean) {
        if (isEan13(ean)) {
            return Integer.parseInt(ean.substring(6, 12));
        }

        return 0;
    }
}
