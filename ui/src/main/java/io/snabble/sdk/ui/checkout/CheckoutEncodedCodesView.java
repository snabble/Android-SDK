package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import io.snabble.sdk.ui.utils.OneShotClickListener;

class CheckoutEncodedCodesView extends FrameLayout implements View.OnLayoutChangeListener {
    private FrameLayout scrollContainer;
    private TextView explanationText;
    private TextView explanationText2;
    private Project project;
    private EncodedCodesGenerator encodedCodesGenerator;
    private int maxSizeMm;

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

        explanationText = findViewById(R.id.explanation1);
        explanationText2 = findViewById(R.id.explanation2);

        maxSizeMm = options.maxSizeMm;
        encodedCodesGenerator = new EncodedCodesGenerator(options);

        final Button paidButton = findViewById(R.id.paid);

        paidButton.setVisibility(View.INVISIBLE);
        paidButton.setAlpha(0);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                paidButton.setVisibility(View.VISIBLE);
                paidButton.animate().setDuration(150).alpha(1).start();
                paidButton.setEnabled(true);
            }
        }, 5000);

        paidButton.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
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

        PriceFormatter priceFormatter = project.getPriceFormatter();

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
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.gravity = Gravity.CENTER;
                        scrollContainer.addView(new CodeListView(getContext()), lp);
                    }

                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int dpHeight = Math.round(scrollContainer.getHeight() / dm.density);
                    if (dpHeight < 220) {
                        explanationText.setVisibility(View.GONE);
                        explanationText2.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private class CodeListView extends RecyclerView {
        public CodeListView(@NonNull Context context) {
            super(context);

            setLayoutManager(new LinearLayoutManager(getContext()));
            setAdapter(new CodeListViewAdapter());
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private class CodeListViewAdapter extends RecyclerView.Adapter {
        private ArrayList<String> codes;

        CodeListViewAdapter() {
            Checkout checkout = project.getCheckout();

            for (String code : checkout.getCodes()) {
                encodedCodesGenerator.add(code);
            }

            encodedCodesGenerator.add(project.getShoppingCart());
            codes = encodedCodesGenerator.generate();
            updateExplanationText(codes.size());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(new BarcodeView(getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            BarcodeView barcodeView = (BarcodeView)holder.itemView;

            int h = scrollContainer.getHeight();
            int barcodeHeight = codes.size() == 1 ? h : h - h / 5;

            if (maxSizeMm > 0) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                float pixelsPerMmX = dm.xdpi / 25.4f;
                int size = (int) (pixelsPerMmX * maxSizeMm);
                size = Math.min(barcodeHeight, size);

                barcodeView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size));
            } else {
                barcodeView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        barcodeHeight));
            }

            barcodeView.setFormat(BarcodeFormat.QR_CODE);

            int padding = Math.round(12.0f * getResources().getDisplayMetrics().density);
            barcodeView.setPadding(padding, padding, padding, padding);

            barcodeView.setText(codes.get(position));
        }

        @Override
        public int getItemCount() {
            return codes.size();
        }
    }
}
