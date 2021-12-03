package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.CheckoutPOSFragment;

public class CheckoutPOSActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutPOSFragment();
    }
}

