package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.integration.SelfScanningFragment;
import io.snabble.sdk.ui.integration.ZebraSupport;
import io.snabble.sdk.ui.scanner.ProductResolver;

public abstract class BaseActivity extends AppCompatActivity implements SnabbleUI.Callback {

    private ProgressBar progressIndicator;
    private View content;
    private TextView sdkError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            App.get().initBlocking();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressIndicator = findViewById(R.id.progress_indicator);
        content = findViewById(R.id.content);
        sdkError = findViewById(R.id.sdk_error);

        App.get().init(new App.InitCallback() {
            @Override
            public void done() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setVisibility(View.GONE);
                        content.setVisibility(View.VISIBLE);

                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.content, onCreateFragment())
                                .commitAllowingStateLoss();
                    }
                });
            }

            @Override
            public void error(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setVisibility(View.GONE);
                        sdkError.setVisibility(View.VISIBLE);
                        sdkError.setText(text);
                    }
                });
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String code = ZebraSupport.dispatchKeyEvent(this, event);
        if (code != null) {
            new ProductResolver.Builder(this)
                    .setCodes(ScannedCode.parse(SnabbleUI.getProject(), code))
                    .create()
                    .show();

            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        SnabbleUI.registerUiCallbacks(this);

        super.onResume();
    }

    @Override
    protected void onStop() {
        SnabbleUI.unregisterUiCallbacks(this);

        super.onStop();
    }

    public abstract Fragment onCreateFragment();

    @Override
    public void execute(SnabbleUI.Action action, Object data) {
        switch(action) {
            case GO_BACK:
                onBackPressed();
                break;
            case SHOW_SCANNER:
                showScannerWithCode((String)data);
                break;
            case SHOW_SHOPPING_CART:
                showShoppingCart();
                break;
            case SHOW_BARCODE_SEARCH:
                showBarcodeSearch();
                break;
            case SHOW_CHECKOUT_GATEKEEPER:
                showCheckoutGatekeeper();
                break;
            case SHOW_CHECKOUT_ONLINE:
                showCheckoutOnline();
                break;
            case SHOW_CHECKOUT_OFFLINE:
                showCheckoutOffline();
                break;
            case SHOW_CHECKOUT_CUSTOMERCARD:
                showCheckoutCustomerCard();
                break;
            case SHOW_CHECKOUT_POINT_OF_SALE:
                showCheckoutQRCodePOS();
                break;
            case SHOW_PAYMENT_FAILURE:
                showPaymentFailure();
                break;
            case SHOW_PAYMENT_SUCCESS:
                showPaymentSuccess();
                break;
            case SHOW_SEPA_CARD_INPUT:
                showSEPACardInput();
                break;
            case SHOW_CREDIT_CARD_INPUT:
                showCreditCardInput();
                break;
            case SHOW_PAYMENT_CREDENTIALS_LIST:
                showPaymentCredentialsList();
                break;
            case SHOW_AGE_VERIFICATION:
                showAgeVerification();
                break;
        }
    }

    public void showShoppingCart() {
        Intent intent = new Intent(this, ShoppingCartActivity.class);
        startActivity(intent);
    }

    public void showScanner() {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void showCheckoutOffline() {
        Intent intent = new Intent(this, CheckoutOfflineActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showCheckoutCustomerCard() {
        Intent intent = new Intent(this, CheckoutCustomerCardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showCheckoutGatekeeper() {
        Intent intent = new Intent(this, CheckoutGatekeeperActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showCheckoutOnline() {
        Intent intent = new Intent(this, CheckoutOnlineActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showCheckoutQRCodePOS() {
        Intent intent = new Intent(this, CheckoutPOSActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showPaymentSuccess() {
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showPaymentFailure() {
        Intent intent = new Intent(this, PaymentFailureActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showScannerWithCode(String scannableCode) {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SelfScanningFragment.ARG_SHOW_PRODUCT_CODE, scannableCode);
        startActivity(intent);
    }

    public void showBarcodeSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showSEPACardInput() {
        Intent intent = new Intent(this, SEPACardInputActivity.class);
        startActivity(intent);
    }

    public void showCreditCardInput() {
        Intent intent = new Intent(this, CreditCardInputActivity.class);
        startActivity(intent);
    }

    public void showPaymentCredentialsList() {
        Intent intent = new Intent(this, PaymentCredentialsListActivity.class);
        startActivity(intent);
    }

    public void showAgeVerification() {
        Intent intent = new Intent(this, AgeVerificationActivity.class);
        startActivity(intent);
    }

}
