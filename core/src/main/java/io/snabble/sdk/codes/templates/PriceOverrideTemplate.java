package io.snabble.sdk.codes.templates;

public class PriceOverrideTemplate {
    private CodeTemplate codeTemplate;
    private CodeTemplate transmissionCodeTemplate;
    private String transmissionCode;

    public PriceOverrideTemplate(CodeTemplate codeTemplate, CodeTemplate transmissionCodeTemplate, String transmissionCode) {
        this.codeTemplate = codeTemplate;
        this.transmissionCodeTemplate = transmissionCodeTemplate;
        this.transmissionCode = transmissionCode;
    }

    public CodeTemplate getCodeTemplate() {
        return codeTemplate;
    }

    public CodeTemplate getTransmissionCodeTemplate() {
        return transmissionCodeTemplate;
    }

    public String getTransmissionCode() {
        return transmissionCode;
    }
}
