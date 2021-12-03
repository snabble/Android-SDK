package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.PaydirektInputFragment;

public class PaydirektInputActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new PaydirektInputFragment();
    }
}

