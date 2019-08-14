package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.CreditCardInputFragment;

public class CreditCardInputActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new CreditCardInputFragment();
    }
}

