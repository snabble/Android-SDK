package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.integration.SelfScanningFragment;

public abstract class BaseActivity extends AppCompatActivity implements SnabbleUICallback {
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
    public void showMainscreen() {
        Intent intent = new Intent(this, MainActivity.class);
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
}
