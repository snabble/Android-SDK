package io.snabble.testapp;

import androidx.fragment.app.Fragment;
import io.snabble.sdk.ui.payment.ProjectPaymentOptionsFragment;

public class ProjectPaymentOptionsActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        ProjectPaymentOptionsFragment fragment = new ProjectPaymentOptionsFragment();
        fragment.setArguments(getIntent().getBundleExtra("args"));
        return fragment;
    }
}

