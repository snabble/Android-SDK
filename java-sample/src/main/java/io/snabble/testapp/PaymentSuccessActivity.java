package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.PaymentSuccessFragment;

public class PaymentSuccessActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        return new PaymentSuccessFragment();
    }
}

