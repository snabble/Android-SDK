package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN14;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EAN14Group extends CodeGroup {
    public EAN14Group(CodeTemplate template) {
        super(template, 14);
    }

    @Override
    public boolean validate() {
        return EAN14.isEan14(data());
    }
}
