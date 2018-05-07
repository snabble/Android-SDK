package io.snabble.sdk.ui.checkout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;

public class CheckoutView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private ViewAnimator viewAnimator;
    private Checkout checkout;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Checkout.State previousState;
    private DelayedProgressDialog progressDialog;

    private DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            checkout.cancel();
        }
    };

    public CheckoutView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_checkout, this);

        viewAnimator = findViewById(R.id.view_animator);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);

        checkout = SnabbleUI.getSdkInstance().getCheckout();

        onStateChanged(checkout.getState());
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.VERIFYING_PAYMENT_METHOD) {
            progressDialog.setOnCancelListener(onCancelListener);
            progressDialog.showAfterDelay(500);
        } else {
            progressDialog.dismiss();
        }

        switch (state) {
            case REQUEST_PAYMENT_METHOD:
                displayView(new PaymentMethodView(getContext()));
                break;
            case WAIT_FOR_APPROVAL:
                displayPaymentView();
                break;
            case PAYMENT_APPROVED:
                if (previousState != null && checkout.getSelectedPaymentMethod() == PaymentMethod.CASH) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            displayView(new CheckoutDoneView(getContext()));
                        }
                    }, 3000);
                } else {
                    if(checkout.getSelectedPaymentMethod() != PaymentMethod.ENCODED_CODES) {
                        displayView(new CheckoutDoneView(getContext()));
                    }
                }
                break;
            case PAYMENT_ABORTED:
            case DENIED_BY_PAYMENT_PROVIDER:
            case DENIED_BY_SUPERVISOR:
                displayView(new CheckoutAbortedView(getContext()));
                break;
        }

        previousState = state;
    }

    private void displayPaymentView() {
        switch(checkout.getSelectedPaymentMethod()){
            case CASH:
                displayView(new CheckoutStatusView(getContext()));
                break;
            case QRCODE_POS:
                CheckoutQRCodePOSView checkoutQRCodePOSView = new CheckoutQRCodePOSView(getContext());
                checkoutQRCodePOSView.setQRCodeText(checkout.getQRCodePOSContent());
                displayView(checkoutQRCodePOSView);
                break;
            case ENCODED_CODES:
                displayView(new CheckoutEncodedCodesView(getContext()));
                break;
        }
    }

    public void displayView(View view) {
        viewAnimator.removeAllViews();
        viewAnimator.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        checkout.addOnCheckoutStateChangedListener(this);

        progressDialog.setOnCancelListener(onCancelListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        checkout.removeOnCheckoutStateChangedListener(this);

        progressDialog.setOnCancelListener(null);
        progressDialog.dismiss();
    }
}
