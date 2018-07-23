package io.snabble.testapp;

import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.CheckoutFragment;
import io.snabble.sdk.ui.integration.ProductSearchFragment;

public class CheckoutActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new CheckoutFragment();
    }
}

