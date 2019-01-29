package io.snabble.sdk.codes;

import android.os.SystemClock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Project;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.utils.Logger;

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

    public static List<ScannableCode> parse(Project project, String code) {
        List<ScannableCode> matches = new ArrayList<>();

        long time = SystemClock.elapsedRealtime();

        for (CodeTemplate codeTemplate : project.getCodeTemplates()) {
            ScannableCode scannableCode = codeTemplate.match(code);
            if (scannableCode != null) {
                matches.add(scannableCode);
            }
        }

        Logger.d("parse took: %dms", SystemClock.elapsedRealtime() - time);

        return matches;
    }
}
