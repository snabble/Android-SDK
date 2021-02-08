package io.snabble.testapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import io.snabble.sdk.ui.integration.ProjectPaymentOptionsFragment;

public class ProjectPaymentOptionsActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        ProjectPaymentOptionsFragment fragment = new ProjectPaymentOptionsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ProjectPaymentOptionsFragment.ARG_BRAND, getIntent().getSerializableExtra(ProjectPaymentOptionsFragment.ARG_BRAND));
        return fragment;
    }
}

