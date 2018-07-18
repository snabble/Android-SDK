package io.snabble.testapp;

import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.UserPaymentMethodListFragment;
import io.snabble.sdk.ui.integration.UserPaymentMethodSelectFragment;

public class UserPaymentMethodSelectActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new UserPaymentMethodSelectFragment();
    }
}

