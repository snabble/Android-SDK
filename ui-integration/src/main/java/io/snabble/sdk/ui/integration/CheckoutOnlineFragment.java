package io.snabble.sdk.ui.integration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.checkout.CheckoutOnlineView;

public class CheckoutOnlineFragment extends Fragment {
    private CheckoutOnlineView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = (CheckoutOnlineView) inflater.inflate(R.layout.snabble_fragment_checkout_online, container, false);
        return view;
    }
}
