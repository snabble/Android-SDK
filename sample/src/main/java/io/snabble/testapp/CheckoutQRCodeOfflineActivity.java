package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutQRCodeOfflineFragment;

public class CheckoutQRCodeOfflineActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutQRCodeOfflineFragment();
    }
}

