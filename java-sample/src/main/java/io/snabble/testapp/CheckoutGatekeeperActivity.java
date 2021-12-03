package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.CheckoutGatekeeperFragment;

public class CheckoutGatekeeperActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutGatekeeperFragment();
    }
}

