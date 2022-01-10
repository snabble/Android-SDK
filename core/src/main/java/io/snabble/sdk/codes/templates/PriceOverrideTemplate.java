package io.snabble.sdk.codes.templates;

public class PriceOverrideTemplate {
    private final CodeTemplate codeTemplate;
    private final CodeTemplate transmissionCodeTemplate;
    private final String transmissionCode;

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