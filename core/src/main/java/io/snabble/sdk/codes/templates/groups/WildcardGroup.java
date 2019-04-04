package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class WildcardGroup extends CodeGroup {
    public WildcardGroup(CodeTemplate template, int length) {
        super(template, length);
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public boolean apply(String input) {
        setLength(input.length());
        return super.apply(input);
    }
}
