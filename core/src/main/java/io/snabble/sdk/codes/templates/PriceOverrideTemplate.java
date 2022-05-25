package io.snabble.sdk.codes.templates;

/**
 * Class for converting regular codes to codes with embedded prices.
 */
public class PriceOverrideTemplate {
    private final CodeTemplate codeTemplate;
    private final CodeTemplate transmissionCodeTemplate;
    private final String transmissionCode;

    public PriceOverrideTemplate(CodeTemplate codeTemplate, CodeTemplate transmissionCodeTemplate, String transmissionCode) {
        this.codeTemplate = codeTemplate;
        this.transmissionCodeTemplate = transmissionCodeTemplate;
        this.transmissionCode = transmissionCode;
    }

    /**
     * The code template that initially will be used to parse the scanned product
     */
    public CodeTemplate getCodeTemplate() {
        return codeTemplate;
    }

    /**
     * The code template that will be used to transmit it over encoded codes
     */
    public CodeTemplate getTransmissionCodeTemplate() {
        return transmissionCodeTemplate;
    }

    /**
     * A fixed transmission code, as a replacement
     */
    public String getTransmissionCode() {
        return transmissionCode;
    }
}