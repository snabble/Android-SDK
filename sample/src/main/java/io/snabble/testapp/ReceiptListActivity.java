package io.snabble.testapp;

import androidx.fragment.app.Fragment;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.ProductSearchFragment;
import io.snabble.sdk.ui.integration.ReceiptListFragment;

public class ReceiptListActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new ReceiptListFragment();
    }
}

