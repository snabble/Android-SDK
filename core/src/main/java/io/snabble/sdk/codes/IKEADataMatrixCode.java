package io.snabble.sdk.codes;

import org.apache.commons.lang3.StringUtils;
import io.snabble.sdk.Project;

public class IKEADataMatrixCode extends ScannableCode {
    private String lookupCode;
    private int price;

    IKEADataMatrixCode(Project project, String code) {
        super(project, code);

        String[] split = code.split("\u001D");
        String codePart = split[3];
        String pricePart = split[4];

        lookupCode = codePart.substring(3, codePart.length());
        price = Integer.parseInt(pricePart.substring(pricePart.length() - 6, pricePart.length() - 1));
        price *= 100; // price is in EUR
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

    public static boolean isIKEADataMatrixCode(Project project, String code) {
        return project.getId().contains("ikea")
                && StringUtils.countMatches(code, '\u001D') == 4;
    }
}
