package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CreditCardInputFragment;
import io.snabble.sdk.ui.integration.PaydirektInputFragment;

public class PaydirektInputActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new PaydirektInputFragment();
    }
}

