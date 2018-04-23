package io.snabble.testapp;

import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.ProductSearchFragment;
import io.snabble.sdk.ui.integration.SelfScanningFragment;

public class ProductSearchActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new ProductSearchFragment();
    }
}

