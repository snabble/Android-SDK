package io.snabble.testapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.SnabbleUI;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Fragment onCreateFragment() {
        return new HomeFragment();
    }
}
