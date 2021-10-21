package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutGatekeeperFragment;

public class CheckoutGatekeeperActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutGatekeeperFragment();
    }
}

