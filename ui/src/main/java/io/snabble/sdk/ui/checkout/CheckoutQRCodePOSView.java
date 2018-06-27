package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeFormat;
import io.snabble.sdk.ui.scanner.BarcodeView;

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

        barcodeView = findViewById(R.id.qr_code);

        findViewById(R.id.abort).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleUI.getSdkInstance().getCheckout().cancel();
            }
        });

        SnabbleSdk sdkInstance = SnabbleUI.getSdkInstance();

        PriceFormatter priceFormatter = new PriceFormatter(sdkInstance);
        String formattedAmount = priceFormatter.format(sdkInstance.getCheckout().getPriceToPay());

        TextView textView = findViewById(R.id.pay_amount);
        textView.setText(getContext().getString(R.string.Snabble_PaymentSelection_title) + " " + formattedAmount);
    }

    public void setQRCodeText(String text) {
        barcodeView.setFormat(BarcodeFormat.QR_CODE);
        barcodeView.setText(text);
    }
}
