package io.snabble.sdk.ui;

public interface SnabbleUICallback {
    void showCheckout();

    void showMainscreen();

    void showScannerWithCode(String scannableCode);

    void showBarcodeSearch();

    void showSEPACardInput();

    void showUserPaymentMethodList();

    void showUserPaymentMethodSelect();

    void goBack();
}
