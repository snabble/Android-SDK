package io.snabble.sdk.codes.templates.groups;

import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class InternalChecksumGroup extends NumberGroup {
    public InternalChecksumGroup(CodeTemplate template) {
        super(template, 1);
    }

    @Override
    public boolean checkDependencies() {
        WeightGroup weightComponent = getTemplate().getComponent(WeightGroup.class);
        if (weightComponent == null || weightComponent.length() != 5) {
            return false;
        }

        return true;
    }

    @Override
    public boolean validate() {
        WeightGroup weightComponent = getTemplate().getComponent(WeightGroup.class);
        if (weightComponent != null) {
            String weight = weightComponent.data();
            return EAN13.internalChecksum(weight, 0) == number(data());
        }

        return false;
    }
}
