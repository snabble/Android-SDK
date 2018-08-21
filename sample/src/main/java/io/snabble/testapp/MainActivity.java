package io.snabble.testapp;

import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;

public class MainActivity extends BaseActivity implements SnabbleUICallback {
    @Override
    public Fragment onCreateFragment() {
        return new HomeFragment();
    }
}
