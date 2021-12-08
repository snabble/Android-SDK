package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.payment.PayoneInputFragment;

public class PayoneInputActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        PayoneInputFragment fragment =  new PayoneInputFragment();
        fragment.setArguments(getIntent().getBundleExtra("args"));
        return fragment;
    }
}