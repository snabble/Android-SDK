package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.Project;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.utils.OneShotClickListener;

class CheckoutQRCodePOSView extends FrameLayout {
    private BarcodeView barcodeView;

    public CheckoutQRCodePOSView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutQRCodePOSView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutQRCodePOSView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_checkout_qrcode, this);

        Project project = SnabbleUI.getProject();

        barcodeView = findViewById(R.id.qr_code);

        final View abort = findViewById(R.id.abort);
        abort.setVisibility(View.INVISIBLE);
        abort.setAlpha(0);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                abort.setVisibility(View.VISIBLE);
                abort.animate().setDuration(150).alpha(1).start();
                abort.setEnabled(true);
            }
        }, 5000);

        abort.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                SnabbleUI.getProject().getCheckout().cancelSilently();

                SnabbleUICallback uiCallback = SnabbleUI.getUiCallback();
                if (uiCallback != null) {
                    uiCallback.goBack();
                }
            }
        });

        TextView payAmount = findViewById(R.id.pay_amount);
        PriceFormatter priceFormatter = project.getPriceFormatter();

        int priceToPay = project.getCheckout().getPriceToPay();
        if (priceToPay > 0) {
            String formattedAmount = priceFormatter.format(priceToPay);
            String text = getContext().getString(R.string.Snabble_QRCode_total) + " " + formattedAmount;

            payAmount.setText(text);
        } else {
            payAmount.setVisibility(View.GONE);
            findViewById(R.id.explanation2).setVisibility(View.GONE);
        }

        if (SnabbleUI.getActionBar() != null) {
            findViewById(R.id.explanation1).setVisibility(View.GONE);
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_QRCode_showThisCode);
        }

        TextView checkoutId = findViewById(R.id.checkout_id);
        String id = project.getCheckout().getId();
        if (id != null && id.length() >= 4) {
            String text = getResources().getString(R.string.Snabble_Checkout_ID);
            checkoutId.setText(String.format("%s: %s", text, id.substring(id.length() - 4)));
        } else {
            checkoutId.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int dpHeight = Math.round(h / dm.density);
        if (dpHeight < 400) {
            findViewById(R.id.explanation1).setVisibility(View.GONE);
            findViewById(R.id.explanation2).setVisibility(View.GONE);
        }
    }

    public void setQRCodeText(String text) {
        barcodeView.setFormat(BarcodeFormat.QR_CODE);
        barcodeView.setText(text);
    }
}
