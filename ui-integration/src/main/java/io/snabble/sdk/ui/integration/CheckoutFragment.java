package io.snabble.sdk.ui.integration;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.snabble.sdk.ui.SnabbleUI;

public class CheckoutFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkout, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SnabbleUI.getProject().getCheckout().cancel();
    }
}
