package io.snabble.sdk.codes;


public class EAN14  {
    public static boolean isEan14(String code) {
        return code != null && (code.length() == 16 && code.startsWith("01") || code.length() == 14);
    }
}
