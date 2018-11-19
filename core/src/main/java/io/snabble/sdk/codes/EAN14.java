package io.snabble.sdk.codes;

import io.snabble.sdk.Project;

public class EAN14 extends ScannableCode {
    EAN14(Project project, String code) {
        super(project, code);

        if (code.length() == 16) {
            this.code = code.substring(2, code.length());
        }
    }

    public static boolean isEan14(String code) {
        return code != null && (code.length() == 16 && code.startsWith("01") || code.length() == 14);
    }
}
