package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
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
import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Project;
import io.snabble.sdk.codes.EAN13;
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
    private TextView explanationText2;
    private Project project;
    private int codeCount;

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

        project = SnabbleUI.getProject();

        Button paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Project project = SnabbleUI.getProject();
                project.getCheckout().approveOfflineMethod();

                SnabbleUICallback uiCallback = SnabbleUI.getUiCallback();
                if (uiCallback != null) {
                    uiCallback.showMainscreen();
                }
            }
        });

        PriceFormatter priceFormatter = new PriceFormatter(project);
        String formattedAmount = priceFormatter.format(project.getCheckout().getPriceToPay());

        TextView textView = findViewById(R.id.pay_amount);
        textView.setText(getContext().getString(R.string.Snabble_PaymentSelection_title) + " " + formattedAmount);

        explanationText = findViewById(R.id.explanation1);
        explanationText2 = findViewById(R.id.explanation2);
    }

    private void updateExplanationText(int codeCount) {
        if (codeCount > 1) {
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
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            // posting using a handler too force adding after the layout pass.
            // this avoids a bug in <= API 16 where added views while layouting were discarded
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (scrollContainer.getWidth() > 0 && scrollContainer.getHeight() > 0) {
                        scrollView.removeAllViews();
                        CodeListView codeListView = new CodeListView(getContext());
                        scrollView.addView(codeListView, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                    }

                    Resources resources = getResources();

                    int h = Math.round(scrollContainer.getHeight() * resources.getDisplayMetrics().density);
                    if (h < 160) {
                        explanationText.setVisibility(View.GONE);
                        explanationText2.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public class CodeListView extends LinearLayout {
        private StringBuilder stringBuilder;
        private int barcodeHeight;

        public CodeListView(@NonNull Context context) {
            super(context);

            setOrientation(VERTICAL);

            stringBuilder = new StringBuilder();

            int h = scrollContainer.getHeight();
            barcodeHeight = h - h / 5;

            addCodes();

            if (getChildCount() == 0) {
                barcodeHeight = h;
            }

            generateView();
            updateExplanationText(getChildCount());
        }

        private void addCodes() {
            Checkout checkout = project.getCheckout();
            ShoppingCart shoppingCart = project.getShoppingCart();

            for (String code : checkout.getCodes()) {
                addScannableCode(code);
            }

            for (int i = 0; i < shoppingCart.size(); i++) {
                Product product = shoppingCart.getProduct(i);
                if (product.getType() == Product.Type.UserWeighed) {
                    //encoding weight in ean
                    String[] weighItemIds = product.getWeighedItemIds();
                    if (weighItemIds != null && weighItemIds.length > 0) {
                        StringBuilder code = new StringBuilder(weighItemIds[0]);
                        if (code.length() == 13) {
                            StringBuilder embeddedWeight = new StringBuilder();
                            String quantity = String.valueOf(shoppingCart.getQuantity(i));
                            int leadingZeros = 5 - quantity.length();
                            for (int j = 0; j < leadingZeros; j++) {
                                embeddedWeight.append('0');
                            }
                            embeddedWeight.append(quantity);
                            code.replace(7, 12, embeddedWeight.toString());
                            code.setCharAt(6, Character.forDigit(EAN13.internalChecksum(code.toString()), 10));
                            code.setCharAt(12, Character.forDigit(EAN13.checksum(code.substring(0, 12)), 10));
                            addScannableCode(code.toString());
                        }
                    }
                } else {
                    for (int j = 0; j < shoppingCart.getQuantity(i); j++) {
                        addScannableCode(shoppingCart.getScannedCode(i));
                    }
                }
            }
        }

        private void generateView() {
            stringBuilder.append(project.getEncodedCodesSuffix());
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
            codeCount = 0;
        }

        private void addScannableCode(String scannableCode) {
            int requiredLength = scannableCode.length() + project.getEncodedCodesSuffix().length() + 1;
            if (codeCount + 1 > project.getEncodedCodesMaxCodes()
                    || stringBuilder.length() + (requiredLength) > MAX_CHARS) {
                generateView();
            }

            if (stringBuilder.length() == 0) {
                stringBuilder.append(project.getEncodedCodesPrefix());
            } else {
                stringBuilder.append(project.getEncodedCodesSeparator());
            }

            stringBuilder.append(scannableCode);
            codeCount++;
        }
    }
}
