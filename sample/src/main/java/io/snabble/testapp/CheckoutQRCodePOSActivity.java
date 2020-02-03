package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutQRCodePOSFragment;

public class CheckoutQRCodePOSActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutQRCodePOSFragment();
    }
}

