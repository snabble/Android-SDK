package io.snabble.sdk.codes;

public class EAN14 extends ScannableCode {
    EAN14(String code) {
        super(code);

        if (code.length() == 16) {
            this.code = code.substring(2, code.length());
        }
    }

    public static boolean isEan14(String code) {
        return code != null && (code.length() == 16 && code.startsWith("01") || code.length() == 14);
    }
}
