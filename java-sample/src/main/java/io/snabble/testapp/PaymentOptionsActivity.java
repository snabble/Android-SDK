package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.payment.PaymentOptionsFragment;

public class PaymentOptionsActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        return new PaymentOptionsFragment();
    }
}

