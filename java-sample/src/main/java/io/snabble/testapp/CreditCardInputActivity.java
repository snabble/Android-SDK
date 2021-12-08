package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.payment.CreditCardInputFragment;

public class CreditCardInputActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        CreditCardInputFragment fragment =  new CreditCardInputFragment();
        fragment.setArguments(getIntent().getBundleExtra("args"));
        return fragment;
    }
}