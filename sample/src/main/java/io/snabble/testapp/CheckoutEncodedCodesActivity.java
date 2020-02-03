package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutEncodedCodesFragment;

public class CheckoutEncodedCodesActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutEncodedCodesFragment();
    }
}

