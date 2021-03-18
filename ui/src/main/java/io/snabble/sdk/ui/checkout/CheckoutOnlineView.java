package io.snabble.sdk.ui.checkout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;

public class CheckoutOnlineView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private Checkout checkout;
    private BarcodeView checkoutIdCode;
    private View cancel;
    private View cancelProgress;
    private TextView helperText;
    private TextView helperTextNoImage;
    private Checkout.State currentState;
    private ImageView helperImage;
    private View upArrow;
    private View progressIndicator;

    public CheckoutOnlineView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutOnlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutOnlineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        Snabble.getInstance()._setCurrentActivity(UIUtils.getHostActivity(getContext()));

        inflate(getContext(), R.layout.snabble_view_checkout_online, this);

        Project project = SnabbleUI.getProject();

        checkoutIdCode = findViewById(R.id.checkout_id_code);
        cancel = findViewById(R.id.cancel);
        cancelProgress = findViewById(R.id.cancel_progress);

        helperText = findViewById(R.id.helper_text);

        String text = I18nUtils.getString(getResources(), "Snabble.Payment.Online.message");
        if (text != null) {
            helperText.setVisibility(View.VISIBLE);
            helperText.setText(text);
        } else {
            helperText.setVisibility(View.GONE);
        }

        helperTextNoImage = findViewById(R.id.helper_text_no_image);
        helperImage = findViewById(R.id.helper_image);
        upArrow = findViewById(R.id.arrow);
        progressIndicator = findViewById(R.id.progress_indicator);

        cancel.setOnClickListener(v -> {
            checkout.abort();
            cancelProgress.setVisibility(View.VISIBLE);
            cancel.setEnabled(false);
        });

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Payment_confirm);
        }

        TextView checkoutId = findViewById(R.id.checkout_id);
        String id = SnabbleUI.getProject().getCheckout().getId();
        if (id != null && id.length() >= 4) {
            checkoutId.setText(id.substring(id.length() - 4));
        } else {
            checkoutId.setVisibility(View.GONE);
        }

        project.getAssets().get("checkout-online", this::setHelperImage);

        checkout = project.getCheckout();
        onStateChanged(checkout.getState());
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int dpHeight = Math.round(getHeight() / dm.density);
            if (dpHeight < 500) {
                if (helperImage.getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    helperImage.setLayoutParams(new LinearLayout.LayoutParams(
                            Math.round(helperImage.getWidth() * 0.5f),
                            Math.round(helperImage.getHeight() * 0.5f)));
                }
            } else {
                helperImage.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
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

        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback == null) {
            Logger.e("ui action could not be performed: callback is null");
            return;
        }

        switch (state) {
            case WAIT_FOR_APPROVAL:
                checkoutIdCode.setVisibility(View.VISIBLE);
                String id = checkout.getId();
                if (id != null) {
                    checkoutIdCode.setText(id);
                }
                break;
            case PAYMENT_PROCESSING:
                checkoutIdCode.setVisibility(View.GONE);
                helperTextNoImage.setVisibility(View.GONE);
                helperImage.setVisibility(View.GONE);
                upArrow.setVisibility(View.GONE);
                progressIndicator.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.INVISIBLE);
                cancelProgress.setVisibility(View.INVISIBLE);
                break;
            case PAYMENT_ABORT_FAILED:
                cancelProgress.setVisibility(View.INVISIBLE);
                cancel.setEnabled(true);

                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.Snabble_Payment_cancelError_title)
                        .setMessage(R.string.Snabble_Payment_cancelError_message)
                        .setPositiveButton(R.string.Snabble_OK, (dialog, which) -> {
                            dialog.dismiss();
                            checkout.resume();
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                break;
            case PAYMENT_APPROVED:
                if (currentState == Checkout.State.PAYMENT_APPROVED) {
                    break;
                }
                Telemetry.event(Telemetry.Event.CheckoutSuccessful);
                callback.execute(SnabbleUI.Action.SHOW_PAYMENT_SUCCESS, null);
                break;
            case PAYMENT_ABORTED:
                Telemetry.event(Telemetry.Event.CheckoutAbortByUser);
                callback.execute(SnabbleUI.Action.GO_BACK, null);
                break;
            case DENIED_BY_PAYMENT_PROVIDER:
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider);
                callback.execute(SnabbleUI.Action.SHOW_PAYMENT_FAILURE, null);
                break;
            case DENIED_BY_SUPERVISOR:
                Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor);
                callback.execute(SnabbleUI.Action.SHOW_PAYMENT_FAILURE, null);
                break;
        }

        currentState = state;
    }

    public void setHelperImage(Bitmap bitmap) {
        if (bitmap != null) {
            helperImage.setImageBitmap(bitmap);
            upArrow.setVisibility(View.VISIBLE);
            helperImage.setVisibility(View.VISIBLE);
            helperTextNoImage.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.GONE);
        } else {
            upArrow.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            helperTextNoImage.setVisibility(View.VISIBLE);
            progressIndicator.setVisibility(View.GONE);
        }

        if (currentState == Checkout.State.PAYMENT_PROCESSING) {
            checkoutIdCode.setVisibility(View.GONE);
            helperTextNoImage.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            upArrow.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.INVISIBLE);
            cancelProgress.setVisibility(View.INVISIBLE);
        }
    }
}
