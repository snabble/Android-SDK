package io.snabble.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public abstract class BaseActivity extends AppCompatActivity {

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
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.content, onCreateFragment())
                            .commitAllowingStateLoss();
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
    protected void onStart() {
        super.onStart();

        SnabbleUI.setUiCallback(SnabbleUI.Action.GO_BACK, (activity, args) -> onBackPressed());
    }

    @Override
    protected void onStop() {
        super.onStop();

        SnabbleUI.removeAllUiActions();
    }

    public abstract Fragment onCreateFragment();

    public void showShoppingCart() {
        Intent intent = new Intent(this, ShoppingCartActivity.class);
        startActivity(intent);
    }

    public void showScanner() {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void showPaymentCredentialsList(Bundle args) {
        Intent intent = new Intent(this, PaymentCredentialsListActivity.class);
        intent.putExtra("args", args);
        startActivity(intent);
    }

    public void showPaymentOptions() {
        Intent intent = new Intent(this, PaymentOptionsActivity.class);
        startActivity(intent);
    }
}