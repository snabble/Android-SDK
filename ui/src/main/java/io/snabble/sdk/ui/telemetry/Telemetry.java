package io.snabble.sdk.ui.telemetry;

import androidx.annotation.Nullable;

public class Telemetry {
    public enum Event {
        ClickCheckout,
        SelectedPaymentMethod,
        CheckoutSuccessful,
        CheckoutDeniedBySupervisor,
        CheckoutDeniedByPaymentProvider,
        CheckoutFinishByUser,
        CheckoutAbortByUser,
        ManuallyEnteredProduct,
        ScannedProduct,
        ScannedOnlineProduct,
        ScannedUnknownCode,
        ConfirmedProduct,
        RejectedProduct,
        ToggleTorch,
        DeletedFromCart,
        UndoDeleteFromCart,
        CartAmountChanged,
        SelectedBundleProduct,
        PaymentMethodAdded,
        PaymentMethodDeleted,

        // shopping list
        ListCreated,
        ListDeleted,
        ListUndeleted,
        ListRenamed,
        ItemAddedSpeech,
        ItemAddedSearchProduct,
        ItemAddedSearchText,
        ItemAddedScan,
        ItemDeleted,
        ItemUndeleted,
        ItemMarkDone,
        ItemMarkTodo,
        ItemEditQuantity,
        ItemEditText,
        ItemMoved,
        ItemMarkDoneScanner,
        ItemScanNotFound, // unused
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
