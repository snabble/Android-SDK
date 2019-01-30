package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EAN13ChecksumGroup extends Group {
    public EAN13ChecksumGroup(CodeTemplate template) {
        super(template, 1);
    }

    public void recalculate() {
        String data = getTemplate().string();
        String dataWithoutChecksum = data.substring(0, 12);
        apply(Integer.toString(EAN13.checksum(dataWithoutChecksum)));
    }

    @Override
    public boolean validate() {
        return getTemplate().length() == 13;
    }
}
