package io.snabble.sdk.ui.checkout;

import androidx.fragment.app.FragmentActivity;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.payment.Datatrans;
import io.snabble.sdk.utils.Logger;

public class CheckoutHelper {
    public static void displayPaymentView(FragmentActivity activity, Checkout checkout) {
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
                case AMEX:
                case PAYDIREKT:
                case TWINT:
                case POST_FINANCE_CARD:
                case GOOGLE_PAY:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_ONLINE, null);
                    break;
                case GATEKEEPER_TERMINAL:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_GATEKEEPER, null);
                    break;
                case QRCODE_POS:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_POINT_OF_SALE, null);
                    break;
                case CUSTOMERCARD_POS:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_CUSTOMERCARD, null);
                    break;
                case QRCODE_OFFLINE:
                    callback.execute(SnabbleUI.Action.SHOW_CHECKOUT_OFFLINE, null);
                    break;
            }
        }
    }
}
