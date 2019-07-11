package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.OneShotClickListener;

class CheckoutEncodedCodesView extends FrameLayout implements View.OnLayoutChangeListener {
    private FrameLayout scrollContainer;
    private TextView explanationText2;
    private Project project;
    private EncodedCodesGenerator encodedCodesGenerator;
    private CodeListView codeListView;
    private int maxSizeMm;
    private Button paidButton;

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
        inflate(getContext(), R.layout.snabble_view_checkout_encodedcodes, this);

        scrollContainer = findViewById(R.id.scroll_container);

        explanationText2 = findViewById(R.id.explanation2);

        maxSizeMm = options.maxSizeMm;
        encodedCodesGenerator = new EncodedCodesGenerator(options);

        paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                if (isAtLastBarcode()) {
                    Project project = SnabbleUI.getProject();
                    project.getCheckout().approveOfflineMethod();

                    SnabbleUICallback uiCallback = SnabbleUI.getUiCallback();
                    if (uiCallback != null) {
                        uiCallback.goBack();
                    }

                    Telemetry.event(Telemetry.Event.CheckoutFinishByUser);
                } else {
                    final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) codeListView.getLayoutManager();
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                        @Override
                        protected int getVerticalSnapPreference() {
                            return SNAP_TO_START;
                        }
                    };

                    int scrollTo = getNextBarcodeIndex();
                    smoothScroller.setTargetPosition(scrollTo);
                    linearLayoutManager.startSmoothScroll(smoothScroller);
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
            explanationText2.setVisibility(View.GONE);
        }
    }

    public int getNextBarcodeIndex() {
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) codeListView.getLayoutManager();
        if (linearLayoutManager != null) {
            return linearLayoutManager.findLastVisibleItemPosition();
        }

        return 0;
    }

    public boolean isAtLastBarcode() {
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) codeListView.getLayoutManager();
        if (linearLayoutManager != null) {
            return linearLayoutManager.findLastCompletelyVisibleItemPosition() == linearLayoutManager.getItemCount() - 1;
        }

        return true;
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
                    DisplayMetrics dm = getResources().getDisplayMetrics();

                    if (scrollContainer.getWidth() > 0 && scrollContainer.getHeight() > 0) {
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);

                        scrollContainer.removeAllViews();
                        codeListView = new CodeListView(getContext());
                        scrollContainer.addView(codeListView, lp);
                        codeListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                super.onScrolled(recyclerView, dx, dy);

                                CodeListViewAdapter adapter = ((CodeListViewAdapter)codeListView.getAdapter());
                                if (isAtLastBarcode()) {
                                    paidButton.setText(R.string.Snabble_QRCode_didPay);
                                } else {
                                    paidButton.setText(getResources().getString(R.string.Snabble_QRCode_nextCode,
                                            getNextBarcodeIndex() + 1, adapter.getItemCount()));
                                }
                            }
                        });

                        codeListView.setPadding(0, (int) (12 * dm.density), 0, 0);
                        codeListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    }

                    int dpHeight = Math.round(scrollContainer.getHeight() / dm.density);
                    if (dpHeight < 220) {
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
        BarcodeView barcodeView;
        TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            barcodeView = itemView.findViewById(R.id.barcode);
            title = itemView.findViewById(R.id.title);
        }
    }

    private class CodeListViewAdapter extends RecyclerView.Adapter<ViewHolder> {
        private ArrayList<String> codes;

        CodeListViewAdapter() {
            Checkout checkout = project.getCheckout();

            for (String code : checkout.getCodes()) {
                encodedCodesGenerator.add(code);
            }

            encodedCodesGenerator.add(project.getShoppingCart());
            codes = encodedCodesGenerator.generate();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(View.inflate(getContext(), R.layout.snabble_item_encodedcodes, null));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            boolean smallScreen = explanationText2.getVisibility() == View.GONE;
            DisplayMetrics dm = getResources().getDisplayMetrics();

            holder.itemView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            if (position == 0) {
                if (codes.size() > 1) {
                    holder.title.setText(getResources().getString(R.string.Snabble_QRCode_showTheseCodes, getItemCount()));
                } else {
                    holder.title.setText(R.string.Snabble_QRCode_showThisCode);
                }
            } else {
                if (explanationText2.getVisibility() == View.GONE) {
                    holder.title.setVisibility(View.GONE);
                } else {
                    holder.title.setVisibility(View.VISIBLE);
                    holder.title.setText(getResources().getString(R.string.Snabble_QRCode_entry_title, (position + 1)));
                }
            }

            int offsetH = holder.title.getVisibility() != View.GONE ? (int) (60 * dm.density) : 0;
            int h = scrollContainer.getHeight() - offsetH;
            int barcodeHeight = codes.size() == 1 ? h : h - (smallScreen ? 0 : h / 4);

            if (maxSizeMm > 0) {
                float pixelsPerMmX = dm.xdpi / 25.4f;
                int size = (int) (pixelsPerMmX * maxSizeMm);
                size = Math.min(barcodeHeight, size);

                holder.barcodeView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size));
            } else {
                holder.barcodeView.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        barcodeHeight));
            }

            holder.barcodeView.setFormat(BarcodeFormat.QR_CODE);
            holder.barcodeView.setText(codes.get(position));
        }

        @Override
        public int getItemCount() {
            return codes.size();
        }
    }
}
