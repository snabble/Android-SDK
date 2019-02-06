package io.snabble.sdk.codes;

public class EAN8 {
    public static boolean isEan8(String code) {
        if (code == null || code.length() != 8) {
            return false;
        }

        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) {
                return false;
            }
        }

        return checksum(code) == Character.digit(code.charAt(7), 10);
    }

    public static int checksum(String code) {
        int d0 = Character.digit(code.charAt(0), 10);
        int d1 = Character.digit(code.charAt(1), 10);
        int d2 = Character.digit(code.charAt(2), 10);
        int d3 = Character.digit(code.charAt(3), 10);
        int d4 = Character.digit(code.charAt(4), 10);
        int d5 = Character.digit(code.charAt(5), 10);
        int d6 = Character.digit(code.charAt(6), 10);

        int sum1 = d1 + d3 + d5;
        int sum2 = d0 + d2 + d4 + d6;

        int mod10 = (sum1 + 3 * sum2) % 10;
        return (10 - mod10) % 10;
    }
}
