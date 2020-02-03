package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CheckoutOnlineFragment;

public class CheckoutOnlineActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutOnlineFragment();
    }
}

