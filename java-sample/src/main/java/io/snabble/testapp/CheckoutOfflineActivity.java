package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.CheckoutOfflineFragment;

public class CheckoutOfflineActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutOfflineFragment();
    }
}

