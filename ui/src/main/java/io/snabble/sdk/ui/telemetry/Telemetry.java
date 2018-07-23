package io.snabble.sdk.ui.telemetry;

import android.support.annotation.Nullable;

public class Telemetry {
    public enum Event {
        ClickCheckout,
        SelectedPaymentMethod,
        CheckoutSuccessful,
        CheckoutDeniedBySupervisor,
        CheckoutDeniedByPaymentProvider,
        ManuallyEnteredProduct,
        ScannedProduct,
        ScannedOnlineProduct,
        ScannedUnknownCode,
        ConfirmedProduct,
        RejectedProduct,
        ToggleTorch,
        DeletedFromCart,
        UndoDeleteFromCart,
        SelectedBundleProduct,
    }

    private static OnEventListener onEventListener;

    public static void event(Event event) {
        event(event, null);
    }

    public static void event(Event event, Object data) {
        if (onEventListener != null) {
            onEventListener.onEvent(event, data);
        }
    }

    public static void setOnEventListener(OnEventListener onEventListener) {
        Telemetry.onEventListener = onEventListener;
    }

    public interface OnEventListener {
        void onEvent(Event event, @Nullable Object data);
    }
}
