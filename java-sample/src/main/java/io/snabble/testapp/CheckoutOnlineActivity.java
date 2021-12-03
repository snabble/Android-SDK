package io.snabble.testapp;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.fragment.CheckoutOnlineFragment;

public class CheckoutOnlineActivity extends BaseActivity{
    @Override
    public Fragment onCreateFragment() {
        CheckoutOnlineFragment fragment = new CheckoutOnlineFragment();
        return fragment;
    }

    @Override
    public void onBackPressed() {
        if (SnabbleUI.getProject().getCheckout().getState() == Checkout.State.PAYMENT_ABORTED) {
            super.onBackPressed();
        }
    }
}

