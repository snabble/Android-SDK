package io.snabble.sdk.ui.integration;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.checkout.CheckoutView;
import io.snabble.sdk.ui.telemetry.Telemetry;

public class CheckoutFragment extends Fragment {
    private CheckoutView checkoutView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_checkout, container, false);
        checkoutView = v.findViewById(R.id.checkout_view);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Checkout checkout = SnabbleUI.getProject().getCheckout();
        if(checkout.getState() != Checkout.State.PAYMENT_APPROVED
                && checkout.getState() != Checkout.State.NONE) {
            Telemetry.event(Telemetry.Event.CheckoutAbortByUser);
        }
    }

    public CheckoutView getCheckoutView() {
        return checkoutView;
    }
}
