package io.snabble.testapp;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.checkout.CheckoutDoneView;
import io.snabble.sdk.ui.integration.SelfScanningFragment;
import io.snabble.sdk.ui.integration.ZebraSupport;
import io.snabble.sdk.ui.scanner.ProductResolver;
import io.snabble.sdk.ui.utils.KeyguardUtils;

public abstract class BaseActivity extends AppCompatActivity implements SnabbleUICallback {
    private static final int REQUEST_CODE_KEYGUARD = 0;

    private ProgressBar progressIndicator;
    private View content;
    private TextView sdkError;
    private KeyguardHandler keyguardHandler;

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
    protected void onStart() {
        SnabbleUI.registerUiCallbacks(this);

        super.onStart();
    }

    @Override
    protected void onStop() {
        SnabbleUI.unregisterUiCallbacks(this);

        super.onStop();
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

    @Override
    public void showCheckout() {
        Intent intent = new Intent(this, CheckoutActivity.class);
        startActivity(intent);
    }

    @Override
    public void showScannerWithCode(String scannableCode) {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SelfScanningFragment.ARG_SHOW_PRODUCT_CODE, scannableCode);
        startActivity(intent);
    }

    @Override
    public void showBarcodeSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    @Override
    public void showSEPACardInput() {
        Intent intent = new Intent(this, SEPACardInputActivity.class);
        startActivity(intent);
    }

    @Override
    public void showPaymentCredentialsList() {
        Intent intent = new Intent(this, PaymentCredentialsListActivity.class);
        startActivity(intent);
    }

    @Override
    public void showHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void showReceipts() {
        Intent intent = new Intent(this, ReceiptListActivity.class);
        startActivity(intent);
    }

    @Override
    public void requestKeyguard(KeyguardHandler keyguardHandler) {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && KeyguardUtils.isDeviceSecure()) {
            this.keyguardHandler = keyguardHandler;

            Intent authIntent = km.createConfirmDeviceCredentialIntent(null, null);
            startActivityForResult(authIntent, REQUEST_CODE_KEYGUARD);
        } else {
            keyguardHandler.onKeyguardResult(Activity.RESULT_OK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_KEYGUARD) {
            if(keyguardHandler != null) {
                keyguardHandler.onKeyguardResult(resultCode);
                keyguardHandler = null;
            }
        }
    }

    @Override
    public void goBack() {
        onBackPressed();
    }
}
