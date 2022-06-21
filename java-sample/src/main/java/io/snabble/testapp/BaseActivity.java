package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.cart.ShoppingCartActivity;
import io.snabble.sdk.ui.payment.PaymentCredentialsListActivity;
import io.snabble.sdk.ui.payment.PaymentOptionsActivity;
import io.snabble.sdk.ui.scanner.CombinedScannerActivity;
import io.snabble.sdk.ui.scanner.SelfScanningActivity;
import io.snabble.sdk.ui.utils.ZebraSupport;
import io.snabble.sdk.ui.scanner.ProductResolver;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ProgressBar progressIndicator = findViewById(R.id.progress_indicator);
        final View content = findViewById(R.id.content);

        progressIndicator.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, onCreateFragment())
                .commitAllowingStateLoss();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String code = ZebraSupport.dispatchKeyEvent(this, event);
        if (code != null) {
            new ProductResolver.Builder(this)
                    .setCodes(ScannedCode.parse(Snabble.getInstance().getCheckedInProject().getValue(), code))
                    .create()
                    .resolve();

            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public abstract Fragment onCreateFragment();

    public void showShoppingCart() {
        Intent intent = new Intent(this, ShoppingCartActivity.class);
        startActivity(intent);
    }

    public void showScanner() {
        Intent intent = new Intent(this, SelfScanningActivity.class);
        startActivity(intent);
    }

    public void showCombinedScanner() {
        Intent intent = new Intent(this, CombinedScannerActivity.class);
        startActivity(intent);
    }

    public void showPaymentCredentialsList() {
        Intent intent = new Intent(this, PaymentCredentialsListActivity.class);
        startActivity(intent);
    }

    public void showPaymentOptions() {
        Intent intent = new Intent(this, PaymentOptionsActivity.class);
        startActivity(intent);
    }
}