package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class ConstantCodeGroup extends CodeGroup {
    private final String constantCode;

    public ConstantCodeGroup(CodeTemplate template, String constantCode) {
        super(template, constantCode.length());
        this.constantCode = constantCode;
        apply(constantCode);
    }

    @Override
    public void reset() {
        apply(constantCode);
    }

    @Override
    public boolean validate() {
        return string().equals(constantCode);
    }
}