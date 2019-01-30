package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class EmbedGroup extends NumberGroup {
    public EmbedGroup(CodeTemplate template, int length) {
        super(template, length);
    }

    public void applyInt(int i) {
        StringBuilder sb = new StringBuilder(Integer.toString(i));
        while (sb.length() < length()) {
            sb.insert(0, '0');
        }
        apply(sb.toString());
    }
}
