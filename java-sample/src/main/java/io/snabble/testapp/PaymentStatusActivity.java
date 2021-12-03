package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.PaymentStatusFragment;

public class PaymentStatusActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        return new PaymentStatusFragment();
    }
}

