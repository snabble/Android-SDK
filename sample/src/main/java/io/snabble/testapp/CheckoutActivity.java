package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.CheckoutFragment;

public class CheckoutActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutFragment();
    }

    @Override
    public void showMainscreen() {
        super.showMainscreen();
        finish();
    }
}

