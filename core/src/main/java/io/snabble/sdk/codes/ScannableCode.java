package io.snabble.sdk.codes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class ScannableCode implements Serializable {
    private Integer embeddedData;
    private String lookupCode;
    private String code;
    private String templateName;

    private ScannableCode() {

    }

    public String getCode() {
        return code;
    }

    public String getLookupCode() {
        return lookupCode;
    }

    public int getEmbeddedData() {
        return embeddedData != null ? embeddedData : 0;
    }

    public boolean hasEmbeddedData() {
        return embeddedData != null;
    }

    public String getTemplateName() {
        return templateName;
    }

    public static class Builder {
        ScannableCode scannableCode;

        public Builder(String templateName) {
            scannableCode = new ScannableCode();
            scannableCode.templateName = templateName;
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

    public static ScannableCode parseDefault(String code) {
        for (CodeTemplate codeTemplate : Snabble.getInstance().getCodeTemplates()) {
            ScannableCode scannableCode = codeTemplate.match(code).buildCode();
            if (scannableCode != null && scannableCode.getTemplateName().equals("default")) {
                return scannableCode;
            }
        }

        return null;
    }

    public static List<ScannableCode> parse(String code) {
        List<ScannableCode> matches = new ArrayList<>();

        for (CodeTemplate codeTemplate : Snabble.getInstance().getCodeTemplates()) {
            ScannableCode scannableCode = codeTemplate.match(code).buildCode();
            if (scannableCode != null) {
                matches.add(scannableCode);
            }
        }

        return matches;
    }
}
