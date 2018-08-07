package io.snabble.sdk.codes;

import java.io.Serializable;

import io.snabble.sdk.Project;

public class ScannableCode implements Serializable {
    protected String code;

    ScannableCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getLookupCode() {
        return "";
    }

    public int getEmbeddedData() {
        return 0;
    }

    public boolean hasUnitData() {
        return false;
    }

    public boolean hasPriceData() {
        return false;
    }

    public boolean hasWeighData() {
        return false;
    }

    public boolean hasEmbeddedData() {
        return false;
    }

    public boolean isEmbeddedDataOk() {
        return true;
    }

    public static ScannableCode parse(Project project, String code) {
        if (EAN13.isEan13(code)) {
            return new EAN13(code, project);
        } else if (EAN14.isEan14(code)) {
            return new EAN14(code);
        } else {
            return new ScannableCode(code);
        }
    }
}
