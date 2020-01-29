package io.snabble.sdk.ui;

public interface SnabbleUICallback {
    void showCheckout();
    void showScannerWithCode(String scannableCode);
    void showBarcodeSearch();
    void showSEPACardInput();
    void showCreditCardInput();
    void showShoppingCart();
    void showPaymentCredentialsList();
    void goBack();
}
