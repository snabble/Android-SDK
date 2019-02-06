package io.snabble.sdk.codes.templates.groups;

import java.math.BigDecimal;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class EmbedDecimalGroup extends Group {
    private int integerParts;

    public EmbedDecimalGroup(CodeTemplate template, int integerParts, int fractionalParts) {
        super(template, integerParts+fractionalParts);
        this.integerParts = integerParts;
    }

    public BigDecimal decimal() {
        String s = string();
        String i = s.substring(0, integerParts);
        String f = s.substring(integerParts);

        return new BigDecimal(i + "." + f);
    }

    @Override
    public boolean validate() {
        try {
            String s = string();
            if (s.length() >= length()) {
                String i = s.substring(0, integerParts);
                String f = s.substring(integerParts);

                new BigDecimal(i + "." + f);
                return true;
            }
        } catch (NumberFormatException ignored) { }

        return false;
    }
}