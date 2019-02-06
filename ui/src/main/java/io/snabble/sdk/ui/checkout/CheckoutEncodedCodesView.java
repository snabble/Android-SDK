package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;

class CheckoutEncodedCodesView extends FrameLayout implements View.OnLayoutChangeListener {
    private View scrollContainer;
    private ScrollView scrollView;
    private TextView explanationText;
    private TextView explanationText2;
    private Project project;
    private EncodedCodesGenerator encodedCodesGenerator;

    public CheckoutEncodedCodesView(Context context) {
        super(context);

        project = SnabbleUI.getProject();
        inflateView(project.getEncodedCodesOptions());
    }

    public CheckoutEncodedCodesView(Context context, EncodedCodesOptions options) {
        super(context);

        project = SnabbleUI.getProject();
        inflateView(options);
    }

    private void inflateView(EncodedCodesOptions options) {
        inflate(getContext(), R.layout.view_checkout_encodedcodes, this);

        scrollContainer = findViewById(R.id.scroll_container);
        scrollView = findViewById(R.id.scroll_view);

        explanationText = findViewById(R.id.explanation1);
        explanationText2 = findViewById(R.id.explanation2);

        encodedCodesGenerator = new EncodedCodesGenerator(options);

        Button paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Project project = SnabbleUI.getProject();
                project.getCheckout().approveOfflineMethod();

                SnabbleUICallback uiCallback = SnabbleUI.getUiCallback();
                if (uiCallback != null) {
                    uiCallback.goBack();
                }

                Telemetry.event(Telemetry.Event.CheckoutFinishByUser);
            }
        });


        TextView payAmount = findViewById(R.id.pay_amount);

        PriceFormatter priceFormatter = new PriceFormatter(project);

        int priceToPay = project.getCheckout().getPriceToPay();
        if (priceToPay > 0) {
            String formattedAmount = priceFormatter.format(priceToPay);
            String text = getContext().getString(R.string.Snabble_QRCode_total) + " " + formattedAmount;
            payAmount.setText(text);
        } else {
            payAmount.setVisibility(View.GONE);
            explanationText2.setVisibility(View.GONE);
        }
    }

    private void updateExplanationText(int codeCount) {
        int resId;
        if (codeCount > 1) {
            resId = R.string.Snabble_QRCode_showTheseCodes;
        } else {
            resId = R.string.Snabble_QRCode_showThisCode;
        }

        if (SnabbleUI.getActionBar() != null) {
            explanationText.setVisibility(View.GONE);
            SnabbleUI.getActionBar().setTitle(resId);
        } else {
            explanationText.setText(resId);
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
        private int barcodeHeight;

        public CodeListView(@NonNull Context context) {
            super(context);

            setOrientation(VERTICAL);

            addCodes();
            generateView();
            updateExplanationText(getChildCount());
        }

        private void addCodes() {
            Checkout checkout = project.getCheckout();

            for (String code : checkout.getCodes()) {
                encodedCodesGenerator.add(code);
            }

            encodedCodesGenerator.add(project.getShoppingCart());
        }

        private void generateView() {
            ArrayList<String> codes = encodedCodesGenerator.generate();

            int h = scrollContainer.getHeight();
            barcodeHeight = codes.size() == 1 ? h : h - h / 5;

            for (String code : codes) {
                BarcodeView barcodeView = new BarcodeView(getContext());
                barcodeView.setFormat(BarcodeFormat.QR_CODE);

                int padding = Math.round(12.0f * getResources().getDisplayMetrics().density);
                barcodeView.setPadding(padding, padding, padding, padding);

                barcodeView.setText(code);

                addView(barcodeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        barcodeHeight));
            }
        }
    }
}
