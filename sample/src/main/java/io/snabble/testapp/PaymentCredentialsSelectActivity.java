package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.PaymentCredentialsSelectFragment;

public class PaymentCredentialsSelectActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new PaymentCredentialsSelectFragment();
    }
}

