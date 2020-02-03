package io.snabble.testapp;

import androidx.fragment.app.Fragment;

public class MainActivity extends BaseActivity {
    @Override
    public Fragment onCreateFragment() {
        return new HomeFragment();
    }
}
