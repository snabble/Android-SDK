package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class EmbedGroup extends NumberGroup {
    private final int multiplier;

    public EmbedGroup(CodeTemplate template, int length, int multiplier) {
        super(template, length);
        this.multiplier = multiplier;
    }

    public void applyInt(int i) {
        i = i / multiplier;

        StringBuilder sb = new StringBuilder(Integer.toString(i));
        while (sb.length() < length()) {
            sb.insert(0, '0');
        }
        apply(sb.toString());
    }

    @Override
    public int number() {
        return super.number() * multiplier;
    }
}
