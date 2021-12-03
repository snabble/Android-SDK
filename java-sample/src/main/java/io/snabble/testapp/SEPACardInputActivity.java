package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.fragment.SEPACardInputFragment;

public class SEPACardInputActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new SEPACardInputFragment();
    }
}

