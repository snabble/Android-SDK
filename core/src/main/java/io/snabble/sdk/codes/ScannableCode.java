package io.snabble.sdk.codes;

import java.io.Serializable;

import io.snabble.sdk.Project;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class ScannableCode implements Serializable {
    private Integer embeddedData;
    private String lookupCode;
    private String code;
    private CodeTemplate codeTemplate;

    private ScannableCode() {

    }

    public String getCode() {
        return code;
    }

    public String getLookupCode() {
        return lookupCode;
    }

    public String getMaskedCode() {
        return "";
    }

    public int getEmbeddedData() {
        return embeddedData != null ? embeddedData : 0;
    }

    public boolean hasEmbeddedData() {
        return embeddedData != null;
    }

    public CodeTemplate getCodeTemplate() {
        return codeTemplate;
    }

    public static class Builder {
        ScannableCode scannableCode;

        public Builder(CodeTemplate codeTemplate) {
            scannableCode = new ScannableCode();
            scannableCode.codeTemplate = codeTemplate;
        }

        public Builder setScannedCode(String scannedCode) {
            scannableCode.code = scannedCode;
            return this;
        }

        public Builder setLookupCode(String lookupCode) {
            scannableCode.lookupCode = lookupCode;
            return this;
        }

        public Builder setEmbeddedData(int embeddedData) {
            scannableCode.embeddedData = embeddedData;
            return this;
        }

        public ScannableCode create() {
            return scannableCode;
        }
    }

    public static ScannableCode parse(Project project, String code) {
        for (CodeTemplate codeTemplate : project.getCodeTemplates()) {
            ScannableCode scannableCode = codeTemplate.match(code);
            if (scannableCode != null) {
                return scannableCode;
            }
        }

        return null;
    }
}
