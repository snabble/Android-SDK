package io.snabble.testapp;

import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.UserPaymentMethodListFragment;

public class UserPaymentMethodListActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new UserPaymentMethodListFragment();
    }
}

