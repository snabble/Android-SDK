package io.snabble.sdk.codes;

import io.snabble.sdk.Project;

public class EdekaProductCode extends ScannableCode {
    private String lookupCode;
    private int price;

    private String overriddenCode;

    EdekaProductCode(Project project, String code) {
        super(code);

        lookupCode = code.substring(2, 15);
        price = Integer.parseInt(code.substring(15, 21));
        overriddenCode = EAN13.generateNewCodeWithEmbeddedData(project, "2417000000000", price).getCode();
    }

    @Override
    public String getCode() {
        return overriddenCode;
    }

    @Override
    public String getLookupCode() {
        return lookupCode;
    }

    @Override
    public int getEmbeddedData() {
        return price;
    }

    @Override
    public boolean hasPriceData() {
        return true;
    }

    @Override
    public boolean hasEmbeddedData() {
        return true;
    }

    @Override
    public boolean isEmbeddedDataOk() {
        return true;
    }

    public static boolean isEdekaProductCode(Project project, String code) {
        return project.getId().contains("edeka") && code.length() == 22 && code.startsWith("97");
    }
}
