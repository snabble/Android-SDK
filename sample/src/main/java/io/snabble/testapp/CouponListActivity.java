package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.CouponListFragment;
import io.snabble.sdk.ui.integration.ShoppingCartFragment;

public class CouponListActivity extends BaseActivity  {
    @Override
    public Fragment onCreateFragment() {
        return new CouponListFragment();
    }
}

