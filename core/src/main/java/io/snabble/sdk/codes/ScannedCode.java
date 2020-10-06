package io.snabble.sdk.codes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Project;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.codes.templates.PriceOverrideTemplate;

public class ScannedCode implements Serializable {
    private Integer embeddedData;
    private BigDecimal embeddedDecimalData;
    private Integer price;
    private String lookupCode;
    private String code;
    private String templateName;

    private Unit embeddedUnit;
    private String transformationTemplateName;
    private String transformationCode;

    private ScannedCode() {

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

    public void setEmbeddedData(int embeddedData) {
        this.embeddedData = embeddedData;
    }

    public BigDecimal getEmbeddedDecimalData() {
        return embeddedDecimalData != null ? embeddedDecimalData : BigDecimal.ZERO;
    }

    public boolean hasEmbeddedDecimalData() {
        return embeddedDecimalData != null;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Unit getEmbeddedUnit() {
        return embeddedUnit;
    }

    public void setEmbeddedUnit(Unit embeddedUnit) {
        this.embeddedUnit = embeddedUnit;
    }

    public String getTransformationCode() {
        return transformationCode;
    }

    public String getTransformationTemplateName() {
        return transformationTemplateName;
    }

    public int getPrice() {
        return price != null ? price : 0;
    }

    public boolean hasPrice() {
        return price != null;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        ScannedCode scannedCode;

        public Builder(String templateName) {
            scannedCode = new ScannedCode();
            scannedCode.templateName = templateName;
        }

        public Builder(ScannedCode source) {
            scannedCode = new ScannedCode();
            scannedCode.embeddedData = source.embeddedData;
            scannedCode.embeddedDecimalData = source.embeddedDecimalData;
            scannedCode.price = source.price;
            scannedCode.lookupCode = source.lookupCode;
            scannedCode.code = source.code;
            scannedCode.templateName = source.templateName;
            scannedCode.embeddedUnit = source.embeddedUnit;
            scannedCode.transformationTemplateName = source.transformationTemplateName;
            scannedCode.transformationCode = source.transformationCode;
        }

        public Builder setScannedCode(String scannedCode) {
            this.scannedCode.code = scannedCode;
            return this;
        }

        public Builder setLookupCode(String lookupCode) {
            scannedCode.lookupCode = lookupCode;
            return this;
        }

        public Builder setEmbeddedData(int embeddedData) {
            scannedCode.embeddedData = embeddedData;
            return this;
        }

        public Builder setEmbeddedDecimalData(BigDecimal embeddedDecimalData) {
            scannedCode.embeddedDecimalData = embeddedDecimalData;
            return this;
        }

        public Builder setEmbeddedUnit(Unit unit) {
            scannedCode.embeddedUnit = unit;
            return this;
        }

        public Builder setTransformationCode(String transformationCode) {
            scannedCode.transformationCode = transformationCode;
            return this;
        }

        public Builder setTransformationTemplateName(String name) {
            scannedCode.transformationTemplateName = name;
            return this;
        }

        public Builder setPrice(int price) {
            scannedCode.price = price;
            return this;
        }

        public ScannedCode create() {
            return scannedCode;
        }
    }

    public static ScannedCode parseDefault(Project project, String code) {
        for (CodeTemplate codeTemplate : project.getCodeTemplates()) {
            ScannedCode scannedCode = codeTemplate.match(code).buildCode();
            if (scannedCode != null && scannedCode.getTemplateName().equals("default")) {
                return scannedCode;
            }
        }

        return null;
    }

    public static List<ScannedCode> parse(Project project, String code) {
        List<ScannedCode> matches = new ArrayList<>();
        CodeTemplate defaultTemplate = project.getDefaultCodeTemplate();

        for (CodeTemplate codeTemplate : project.getCodeTemplates()) {
            ScannedCode scannedCode = codeTemplate.match(code).buildCode();
            if (scannedCode != null) {
                matches.add(scannedCode);
            }
        }

        for (PriceOverrideTemplate priceOverrideTemplate : project.getPriceOverrideTemplates()) {
            CodeTemplate codeTemplate = priceOverrideTemplate.getCodeTemplate();
            ScannedCode scannedCode = codeTemplate.match(code).buildCode();
            if (scannedCode != null) {
                String lookupCode = scannedCode.getLookupCode();
                if (!lookupCode.equals(code)) {
                    ScannedCode defaultCode = defaultTemplate.match(lookupCode).buildCode();
                    if (defaultCode != null) {
                        defaultCode.embeddedData = scannedCode.getEmbeddedData();
                        defaultCode.embeddedUnit = Unit.PRICE;
                        defaultCode.code = scannedCode.getCode();

                        CodeTemplate transformTemplate = priceOverrideTemplate.getTransmissionCodeTemplate();
                        if (transformTemplate != null) {
                            defaultCode.transformationTemplateName = transformTemplate.getName();
                            defaultCode.transformationCode = priceOverrideTemplate.getTransmissionCode();
                        }

                        matches.add(defaultCode);
                    }
                }
            }
        }

        return matches;
    }
}
