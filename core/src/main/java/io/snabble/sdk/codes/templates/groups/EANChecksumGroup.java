package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EANChecksumGroup extends Group {
    public EANChecksumGroup(CodeTemplate template) {
        super(template, 1);
    }

    public void recalculate() {
        String data = getTemplate().string();
        String dataWithoutChecksum = data.substring(0, data.length() - 1);
        apply(Integer.toString(EAN13.checksum(dataWithoutChecksum)));
    }

    @Override
    public boolean validate() {
        int len = getTemplate().length();
        return len == 8 || len == 13;
    }
}
