package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.ShoppingCartFragment;

public class ShoppingCartActivity extends BaseActivity  {
    @Override
    public Fragment onCreateFragment() {
        return new ShoppingCartFragment();
    }
}

