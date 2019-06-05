package io.snabble.sdk.ui;

import android.content.Intent;

public interface SnabbleUICallback {
    void showCheckout();
    void showScannerWithCode(String scannableCode);
    void showBarcodeSearch();
    void showSEPACardInput();
    void showCreditCardInput();
    void showShoppingCart();
    void showPaymentCredentialsList();

    /**
     * Request a keyguard for user authentication. This will only be called if
     * {@link io.snabble.sdk.UserPreferences#setRequireKeyguardAuthenticationForPayment(boolean)}
     * is set to true.
     *
     * Typically you request a Keyguard using
     * {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence, CharSequence)}.
     *
     * @param keyguardHandler A KeyguardHandler that needs to be supplied with the result of
     *                        {@link android.app.Activity#onActivityResult(int, int, Intent)}.
     */
    void requestKeyguard(KeyguardHandler keyguardHandler);
    void goBack();
}
