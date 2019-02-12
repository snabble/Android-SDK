package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.core.widget.ImageViewCompat;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;

class CheckoutStatusView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private Checkout checkout;
    private BarcodeView checkoutIdCode;

    public CheckoutStatusView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_checkout_status, this);

        checkoutIdCode = findViewById(R.id.checkout_id_code);

        // zxing uses StandardCharsets class for DATA_MATRIX codes, which is not available
        // before Android 4.4
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            checkoutIdCode.setFormat(BarcodeFormat.QR_CODE);
        }

        View cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkout.cancel();
            }
        });

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Payment_confirm);
        }

        checkout = SnabbleUI.getProject().getCheckout();
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
        switch (state) {
            case WAIT_FOR_APPROVAL:
                String id = checkout.getId();
                if (id != null) {
                    checkoutIdCode.setText(id);
                }
                break;
            case DENIED_BY_PAYMENT_PROVIDER:
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider);
                break;
            case DENIED_BY_SUPERVISOR:
                Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor);
                break;
        }
    }
}
