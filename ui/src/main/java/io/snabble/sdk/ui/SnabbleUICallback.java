package io.snabble.sdk.ui;

public interface SnabbleUICallback {
    void showCheckout();

    void showScannerWithCode(String scannableCode);

    void showBarcodeSearch();

    void showSEPACardInput();

    void showPaymentCredentialsList();

    void goBack();
}
