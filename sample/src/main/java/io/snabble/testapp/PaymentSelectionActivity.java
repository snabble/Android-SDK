package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.PaymentSelectionFragment;

public class PaymentSelectionActivity extends BaseActivity  {
    @Override
    public Fragment onCreateFragment() {
        return new PaymentSelectionFragment();
    }
}

