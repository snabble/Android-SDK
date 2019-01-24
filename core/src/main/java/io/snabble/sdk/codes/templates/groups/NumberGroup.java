package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class NumberGroup extends Group {
    public NumberGroup(CodeTemplate template, int length) {
        super(template, length);
    }

    public int number() {
        try {
            return Integer.parseInt(data());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean validate() {
        try {
            Integer.parseInt(data());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
