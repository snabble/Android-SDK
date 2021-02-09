package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.SelfScanningFragment;

public class SelfScanningActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        SelfScanningFragment selfScanningFragment = new SelfScanningFragment();
        selfScanningFragment.setArguments(getIntent().getBundleExtra("args"));
        return selfScanningFragment;
    }
}

