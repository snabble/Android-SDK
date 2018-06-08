package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.scanner.BarcodeFormat;
import io.snabble.sdk.ui.scanner.BarcodeView;

class CheckoutEncodedCodesView extends FrameLayout implements View.OnLayoutChangeListener {
    private static final int MAX_CHARS = 2953; // qr-code 8 bit max
    private View scrollContainer;
    private ScrollView scrollView;
    private TextView explanationText;
    private SnabbleSdk sdkInstance;

    public CheckoutEncodedCodesView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutEncodedCodesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutEncodedCodesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_checkout_encodedcodes, this);

        scrollContainer = findViewById(R.id.scroll_container);
        scrollView = findViewById(R.id.scroll_view);

        sdkInstance = SnabbleUI.getSdkInstance();
        ShoppingCart shoppingCart = sdkInstance.getShoppingCart();

        Button paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleSdk sdkInstance = SnabbleUI.getSdkInstance();
                sdkInstance.getCheckout().approveOfflineMethod();

                SnabbleUICallback uiCallback = SnabbleUI.getUiCallback();
                if (uiCallback != null) {
                    uiCallback.showMainscreen();
                }
            }
        });

        PriceFormatter priceFormatter = new PriceFormatter(sdkInstance);
        String formattedAmount = priceFormatter.format(shoppingCart.getTotalPrice());

        TextView textView = findViewById(R.id.pay_amount);
        textView.setText(getContext().getString(R.string.Snabble_PaymentSelection_title) + " " + formattedAmount);

        explanationText = findViewById(R.id.explanation1);
    }

    private void updateExplanationText(int codeCount){
        if(codeCount > 1) {
            explanationText.setText(R.string.Snabble_QRCode_showTheseCodes);
        } else {
            explanationText.setText(R.string.Snabble_QRCode_showThisCode);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        scrollContainer.addOnLayoutChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        scrollContainer.removeOnLayoutChangeListener(this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if(left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            if(scrollContainer.getWidth() > 0 && scrollContainer.getHeight() > 0) {
                scrollView.removeAllViews();
                CodeListView codeListView = new CodeListView(getContext());
                scrollView.addView(codeListView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    public class CodeListView extends LinearLayout {
        private StringBuilder stringBuilder;
        private int barcodeHeight;

        public CodeListView(@NonNull Context context) {
            super(context);

            setOrientation(VERTICAL);

            stringBuilder = new StringBuilder();

            Checkout checkout = sdkInstance.getCheckout();
            ShoppingCart shoppingCart = sdkInstance.getShoppingCart();

            int h = scrollContainer.getHeight();
            barcodeHeight = h - h / 5;

            for (String code : checkout.getCodes()) {
                addScannableCode(code);
            }

            for (int i = 0; i < shoppingCart.size(); i++) {
                for (int j = 0; j < shoppingCart.getQuantity(i); j++) {
                    addScannableCode(shoppingCart.getScannedCode(i));
                }
            }

            if(getChildCount() == 0){
                barcodeHeight = h;
            }

            generateView();
            updateExplanationText(getChildCount());
        }

        private void generateView() {
            stringBuilder.append(sdkInstance.getEncodedCodesSuffix());
            String code = stringBuilder.toString();

            BarcodeView barcodeView = new BarcodeView(getContext());
            barcodeView.setFormat(BarcodeFormat.QR_CODE);

            int padding = Math.round(12.0f * getResources().getDisplayMetrics().density);
            barcodeView.setPadding(padding, padding, padding, padding);

            barcodeView.setText(code);

            addView(barcodeView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    barcodeHeight));

            stringBuilder = new StringBuilder();
        }

        private void addScannableCode(String scannableCode) {
            int requiredLength = scannableCode.length() + sdkInstance.getEncodedCodesSuffix().length() + 1;
            if (stringBuilder.length() + (requiredLength) > MAX_CHARS) {
                generateView();
            }

            if(stringBuilder.length() == 0){
                stringBuilder.append(sdkInstance.getEncodedCodesPrefix());
            } else {
                stringBuilder.append(sdkInstance.getEncodedCodesSeperator());
            }

            stringBuilder.append(scannableCode);
        }
    }
}
