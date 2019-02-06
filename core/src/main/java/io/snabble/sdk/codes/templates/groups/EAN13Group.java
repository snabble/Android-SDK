package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EAN13Group extends CodeGroup {
    public EAN13Group(CodeTemplate template) {
        super(template, 13);
    }

    @Override
    public boolean validate() {
        return EAN13.isEan13(string());
    }
}
