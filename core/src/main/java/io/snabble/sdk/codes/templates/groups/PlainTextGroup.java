package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class PlainTextGroup extends Group {
    private String match;

    public PlainTextGroup(CodeTemplate template, String match) {
        super(template, match.length());
        this.match = match;
    }

    @Override
    public boolean validate() {
        return data().equals(match);
    }
}
