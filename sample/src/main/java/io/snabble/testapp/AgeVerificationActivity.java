package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.AgeVerificationInputFragment;

public class AgeVerificationActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new AgeVerificationInputFragment();
    }
}

