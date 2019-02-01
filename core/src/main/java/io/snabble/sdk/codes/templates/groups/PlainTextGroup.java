package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class PlainTextGroup extends Group {
    private String plainText;

    public PlainTextGroup(CodeTemplate template, String plainText) {
        super(template, plainText.length());
        this.plainText = plainText;
        apply(plainText);
    }

    @Override
    public void reset() {
        apply(plainText);
    }

    @Override
    public boolean validate() {
        return string().equals(plainText);
    }
}
