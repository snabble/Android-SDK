package io.snabble.sdk.codes.templates;

public class PriceOverrideTemplate {
    private CodeTemplate codeTemplate;
    private CodeTemplate transmissionCodeTemplate;

    public PriceOverrideTemplate(CodeTemplate codeTemplate, CodeTemplate transmissionCodeTemplate) {
        this.codeTemplate = codeTemplate;
        this.transmissionCodeTemplate = transmissionCodeTemplate;
    }

    public CodeTemplate getCodeTemplate() {
        return codeTemplate;
    }

    public CodeTemplate getTransmissionCodeTemplate() {
        return transmissionCodeTemplate;
    }
}
