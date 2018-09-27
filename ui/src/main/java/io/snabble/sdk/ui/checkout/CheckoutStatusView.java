package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;

class CheckoutStatusView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private ViewGroup[] steps;
    private int currentStep;
    private Checkout checkout;
    private BarcodeView checkoutIdCode;

    private Handler handler = new Handler(Looper.getMainLooper());

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
        currentStep = 0;

        steps = new ViewGroup[3];
        steps[0] = findViewById(R.id.step1);
        steps[1] = findViewById(R.id.step2);
        steps[2] = findViewById(R.id.step3);

        checkoutIdCode = findViewById(R.id.checkout_id_code);
        View cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkout.cancel();
            }
        });

        for(View v : steps) {
            ProgressBar progressBar = v.findViewWithTag("progress");

            int color = 0x1d1f2440;
            progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
        }

        checkout = SnabbleUI.getProject().getCheckout();
        onStateChanged(checkout.getState());
    }

    private void setStep(int step) {
        currentStep = step;

        if (currentStep < steps.length) {
            crossFade(steps[currentStep]);

            int nextStep = currentStep + 1;
            if (nextStep < steps.length) {
                ProgressBar progressBar = steps[nextStep].findViewWithTag("progress");
                progressBar.setIndeterminate(true);
            }
        }
    }

    private void crossFade(final ViewGroup vg) {
        ImageView imageView = vg.findViewWithTag("image");
        ImageView imageView2 = vg.findViewWithTag("image_ok");
        ProgressBar progressBar = vg.findViewWithTag("progress");

        imageView.animate()
                .alpha(0f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(500)
                .start();

        imageView2.animate()
                .alpha(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(500)
                .start();

        progressBar.setVisibility(View.INVISIBLE);
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
                setStep(0);
                break;
            case PAYMENT_APPROVED:
                setStep(1);
                Telemetry.event(Telemetry.Event.CheckoutSuccessful);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setStep(2);
                    }
                }, 1000);
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
