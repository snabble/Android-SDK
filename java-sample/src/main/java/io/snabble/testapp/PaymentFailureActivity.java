package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.PaymentFailureFragment;

public class PaymentFailureActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new PaymentFailureFragment();
    }
}

