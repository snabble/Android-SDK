package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EAN13InternalChecksumGroup extends NumberGroup {
    public EAN13InternalChecksumGroup(CodeTemplate template) {
        super(template, 1);
    }

    @Override
    public boolean checkDependencies() {
        EmbedGroup embedGroup = getTemplate().getGroup(EmbedGroup.class);
        if (embedGroup == null || embedGroup.length() != 5) {
            return false;
        }

        return true;
    }

    public void recalculate() {
        EmbedGroup embedGroup = getTemplate().getGroup(EmbedGroup.class);
        if (embedGroup != null) {
            String weight = embedGroup.string();
            int newChecksum = EAN13.internalChecksum(weight, 0);
            apply(Integer.toString(newChecksum));
        }
    }

    @Override
    public boolean validate() {
        EmbedGroup embedGroup = getTemplate().getGroup(EmbedGroup.class);
        if (embedGroup != null) {
            String weight = embedGroup.string();
            return EAN13.internalChecksum(weight, 0) == number();
        }

        return false;
    }
}
