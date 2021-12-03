package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.CheckoutCustomerCardFragment;

public class CheckoutCustomerCardActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutCustomerCardFragment();
    }
}

