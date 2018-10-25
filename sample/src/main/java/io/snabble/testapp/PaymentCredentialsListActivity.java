package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.PaymentCredentialsListFragment;

public class PaymentCredentialsListActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new PaymentCredentialsListFragment();
    }
}

