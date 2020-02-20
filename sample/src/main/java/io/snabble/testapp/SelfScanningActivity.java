package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.integration.SelfScanningFragment;

public class SelfScanningActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        Intent intent = getIntent();
        String scannableCode = intent.getStringExtra(SelfScanningFragment.ARG_SHOW_PRODUCT_CODE);

        SelfScanningFragment selfScanningFragment = new SelfScanningFragment();
        Bundle args = new Bundle();
        args.putString(SelfScanningFragment.ARG_SHOW_PRODUCT_CODE, scannableCode);
        selfScanningFragment.setArguments(args);

        return selfScanningFragment;
    }
}

