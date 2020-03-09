package io.snabble.sdk.ui.checkout;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.Logger;

public class CheckoutHelper {
    public static void displayPaymentView(Checkout checkout) {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback == null) {
            Logger.e("ui action could not be performed: callback is null");
            return;
        }

        if (checkout != null && checkout.getSelectedPaymentMethod() != null) {
            switch (checkout.getSelectedPaymentMethod()) {
                case TEGUT_EMPLOYEE_CARD:
                case DE_DIRECT_DEBIT:
                case VISA:
                case MASTERCARD:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_ONLINE, null);
                    break;
                case GATEKEEPER_TERMINAL:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_GATEKEEPER, null);
                    break;
                case QRCODE_POS:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_POINT_OF_SALE, null);
                    break;
                case QRCODE_OFFLINE:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_OFFLINE, null);
                    break;
            }
        }
    }
}
