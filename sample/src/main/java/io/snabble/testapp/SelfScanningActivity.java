package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.SelfScanningFragment;

public class SelfScanningActivity extends BaseActivity implements SnabbleUICallback {
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

