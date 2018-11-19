package io.snabble.sdk.codes;

import java.io.Serializable;

import io.snabble.sdk.Project;

public class ScannableCode implements Serializable {
    protected final Project project;
    protected String code;

    ScannableCode(Project project, String code) {
        this.project = project;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getLookupCode() {
        return code;
    }

    public String getMaskedCode() {
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
            return new EAN13(project, code);
        } else if (EAN14.isEan14(code)) {
            return new EAN14(project, code);
        } else if (EdekaProductCode.isEdekaProductCode(project, code)) {
            return new EdekaProductCode(project, code);
        } else if (IKEADataMatrixCode.isIKEADataMatrixCode(project, code)) {
            return new IKEADataMatrixCode(project, code);
        } else  {
            return new ScannableCode(project, code);
        }
    }
}
