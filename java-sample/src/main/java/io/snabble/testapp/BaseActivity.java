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
import io.snabble.sdk.ui.cart.ShoppingCartActivity;
import io.snabble.sdk.ui.checkout.CheckoutActivity;
import io.snabble.sdk.ui.payment.AgeVerificationInputActivity;
import io.snabble.sdk.ui.payment.CreditCardInputActivity;
import io.snabble.sdk.ui.payment.PaydirektInputActivity;
import io.snabble.sdk.ui.payment.PaymentCredentialsListActivity;
import io.snabble.sdk.ui.payment.PaymentOptionsActivity;
import io.snabble.sdk.ui.payment.PayoneInputActivity;
import io.snabble.sdk.ui.payment.ProjectPaymentOptionsActivity;
import io.snabble.sdk.ui.payment.SEPACardInputActivity;
import io.snabble.sdk.ui.scanner.SelfScanningActivity;
import io.snabble.sdk.ui.search.ProductSearchActivity;
import io.snabble.sdk.ui.utils.ZebraSupport;
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
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    sdkError.setVisibility(View.VISIBLE);
                    sdkError.setText(text);
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
                    .resolve();

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
    public void execute(SnabbleUI.Action action, Bundle args) {
        switch(action) {
            case GO_BACK:
                onBackPressed();
                break;
            case SHOW_SCANNER:
                showScannerWithCode(args);
                break;
            case SHOW_SHOPPING_CART:
                showShoppingCart();
                break;
            case SHOW_BARCODE_SEARCH:
                showBarcodeSearch();
                break;
            case SHOW_CHECKOUT:
                showCheckout(args);
                break;
            case SHOW_CHECKOUT_DONE:
                showCheckoutDone();
                break;
            case SHOW_SEPA_CARD_INPUT:
                showSEPACardInput();
                break;
            case SHOW_CREDIT_CARD_INPUT:
                showCreditCardInput(args);
                break;
            case SHOW_PAYONE_INPUT:
                showPayoneInput(args);
                break;
            case SHOW_PAYDIREKT_INPUT:
                showPaydirektInput();
                break;
            case SHOW_PAYMENT_CREDENTIALS_LIST:
                showPaymentCredentialsList(args);
                break;
            case SHOW_PAYMENT_OPTIONS:
                showPaymentOptions();
                break;
            case SHOW_PROJECT_PAYMENT_OPTIONS:
                showProjectPaymentOptions(args);
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
        showScannerWithCode(null);
    }

    public void showCheckout(Bundle args) {
        CheckoutActivity.startCheckoutFlow(this, args);
    }

    public void showCheckoutDone() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showScannerWithCode(Bundle args) {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (args != null) {
            intent.putExtras(args);
        }
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

    public void showCreditCardInput(Bundle args) {
        Intent intent = new Intent(this, CreditCardInputActivity.class);
        if (args != null) {
            intent.putExtras(args);
        }
        startActivity(intent);
    }

    public void showPayoneInput(Bundle args) {
        Intent intent = new Intent(this, PayoneInputActivity.class);
        if (args != null) {
            intent.putExtras(args);
        }
        startActivity(intent);
    }

    public void showPaydirektInput() {
        Intent intent = new Intent(this, PaydirektInputActivity.class);
        startActivity(intent);
    }

    public void showPaymentCredentialsList(Bundle args) {
        Intent intent = new Intent(this, PaymentCredentialsListActivity.class);
        if (args != null) {
            intent.putExtras(args);
        }
        startActivity(intent);
    }

    public void showPaymentOptions() {
        Intent intent = new Intent(this, PaymentOptionsActivity.class);
        startActivity(intent);
    }

    public void showProjectPaymentOptions(Bundle args) {
        Intent intent = new Intent(this, ProjectPaymentOptionsActivity.class);
        if (args != null) {
            intent.putExtras(args);
        }
        startActivity(intent);
    }

    public void showAgeVerification() {
        Intent intent = new Intent(this, AgeVerificationInputActivity.class);
        startActivity(intent);
    }
}