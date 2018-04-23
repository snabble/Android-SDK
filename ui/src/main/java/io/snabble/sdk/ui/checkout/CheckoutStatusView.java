package io.snabble.sdk.ui.checkout;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;

class CheckoutStatusView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private ViewGroup[] steps;
    private int currentStep;

    private View background;
    private View cancel;
    private TextView checkoutId;
    private Checkout checkout;

    private Handler handler = new Handler(Looper.getMainLooper());
    private int backgroundHeight;

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

        background = findViewById(R.id.background);
        cancel = findViewById(R.id.cancel);
        checkoutId = findViewById(R.id.checkout_id);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkout.cancel();
            }
        });

        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int h = background.getHeight();
                if (h > 0 && backgroundHeight == 0) {
                    backgroundHeight = h;
                    reset();
                    onStateChanged(checkout.getState());
                }
            }
        });

        checkout = SnabbleUI.getSdkInstance().getCheckout();
        onStateChanged(checkout.getState());
    }

    private void setStep(int step) {
        currentStep = step;

        if (currentStep < steps.length) {
            animateViewGroup(steps[currentStep]);
        }
    }

    public void reset() {
        currentStep = 0;
        for (ViewGroup vg : steps) {
            resetViewGroup(vg);
        }
    }

    private void resetViewGroup(ViewGroup vg) {
        vg.setBackgroundColor(Color.TRANSPARENT);
        int primaryTextColor = ResourcesCompat.getColor(getResources(), R.color.snabble_textColorDark, null);
        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(primaryTextColor);
            } else if (v instanceof AppCompatImageView) {
                final AppCompatImageView iv = (AppCompatImageView) v;
                ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(primaryTextColor));
            }
        }
    }

    private void animateViewGroup(final ViewGroup vg) {
        int primaryTextColor = ResourcesCompat.getColor(getResources(), R.color.snabble_textColorDark, null);
        int secondaryTextColor = ResourcesCompat.getColor(getResources(), R.color.snabble_textColorLight, null);

        int h = background.getHeight();
        float start = -h + h / 3 * currentStep;
        float end = -h + h / 3 * (currentStep + 1);

        AnimatorSet animatorSet = new AnimatorSet();

        final ObjectAnimator bgAnim = ObjectAnimator.ofFloat(background, "translationY", start, end);
        animatorSet.playTogether(bgAnim);

        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                final ObjectAnimator anim = new ObjectAnimator();
                anim.setIntValues(primaryTextColor, secondaryTextColor);
                anim.setEvaluator(new ArgbEvaluator());
                anim.setTarget(tv);
                anim.setPropertyName("textColor");
                animatorSet.playTogether(anim);
            } else if (v instanceof AppCompatImageView) {
                final AppCompatImageView iv = (AppCompatImageView) v;
                final ObjectAnimator anim = new ObjectAnimator();
                anim.setIntValues(primaryTextColor, secondaryTextColor);
                anim.setEvaluator(new ArgbEvaluator());
                anim.setTarget(iv);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer integer = (Integer) animation.getAnimatedValue();
                        ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(integer));
                    }
                });
                animatorSet.playTogether(anim);
            }
        }

        animatorSet.setDuration(200);
        animatorSet.start();
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
        if (backgroundHeight > 0) {
            switch (state) {
                case WAIT_FOR_APPROVAL:
                    String id = checkout.getId();
                    if (id != null) {
                        id = id.substring(id.length() - 4, id.length());
                        checkoutId.setText("Checkout-ID: " + id);
                    } else {
                        checkoutId.setVisibility(View.INVISIBLE);
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
                    reset();
                    break;
                case DENIED_BY_SUPERVISOR:
                    Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor);
                    reset();
                    break;
                default:
                    reset();
            }
        }
    }
}
