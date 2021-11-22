package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutCustomerCardFragment;

public class CheckoutCustomerCardActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutCustomerCardFragment();
    }
}