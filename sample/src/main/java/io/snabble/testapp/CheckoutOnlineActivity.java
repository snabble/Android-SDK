package io.snabble.testapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import io.snabble.sdk.ui.integration.CheckoutOnlineFragment;

public class CheckoutOnlineActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        CheckoutOnlineFragment fragment = new CheckoutOnlineFragment();
        fragment.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {

            }
        });
        fragment.getViewLifecycleOwnerLiveData().observe(this, new Observer<LifecycleOwner>() {
            @Override
            public void onChanged(LifecycleOwner lifecycleOwner) {
                fragment.getViewLifecycleOwner().getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_CREATE) {
                            //fragment.setHelperImage(R.drawable.snabble_ic_verify_ok);
                        }
                    }
                });

            }
        });

        return fragment;
    }
}

