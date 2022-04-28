package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.utils.Dispatch;

public class CheckoutPointOfSaleView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private BarcodeView barcodeView;
    private Checkout checkout;
    private Checkout.State currentState;

    public CheckoutPointOfSaleView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutPointOfSaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutPointOfSaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_checkout_pos, this);

        Project project = Snabble.getInstance().getCheckedInProject().getValue();

        barcodeView = findViewById(R.id.qr_code);

        final View abort = findViewById(R.id.abort);
        abort.setVisibility(View.INVISIBLE);
        abort.setAlpha(0);

        Dispatch.mainThread(() -> {
            abort.setVisibility(View.VISIBLE);
            abort.animate().setDuration(150).alpha(1).start();
            abort.setEnabled(true);
        }, 5000);

        abort.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                Snabble.getInstance().getCheckedInProject().getValue().getCheckout().abortSilently();
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

        TextView checkoutId = findViewById(R.id.checkout_id);
        String id = project.getCheckout().getId();
        if (id != null && id.length() >= 4) {
            String text = getResources().getString(R.string.Snabble_Checkout_ID);
            checkoutId.setText(String.format("%s: %s", text, id.substring(id.length() - 4)));
        } else {
            checkoutId.setVisibility(View.GONE);
        }

        checkout = Snabble.getInstance().getCheckedInProject().getValue().getCheckout();
        onStateChanged(checkout.getState());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (checkout != null) {
            checkout.addOnCheckoutStateChangedListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (checkout != null) {
            checkout.removeOnCheckoutStateChangedListener(this);
        }
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == currentState) {
            return;
        }

        if (state == Checkout.State.WAIT_FOR_APPROVAL) {
            setQRCodeText(checkout.getQRCodePOSContent());
        }

        currentState = state;
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

    private void setQRCodeText(String text) {
        barcodeView.setFormat(BarcodeFormat.QR_CODE);
        barcodeView.setText(text);
    }
}
