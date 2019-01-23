package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN8;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EAN8Group extends CodeGroup {
    public EAN8Group(CodeTemplate template) {
        super(template, 8);
    }

    @Override
    public boolean validate() {
        return EAN8.isEan8(data());
    }
}
