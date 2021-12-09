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
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.utils.Logger;

public class CheckoutGatekeeperView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private Checkout checkout;
    private BarcodeView checkoutIdCode;
    private View cancel;
    private View cancelProgress;
    private Checkout.State currentState;
    private TextView helperText;
    private ImageView helperImage;
    private View upArrow;
    private View progressIndicator;

    public CheckoutGatekeeperView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutGatekeeperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutGatekeeperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_checkout_gatekeeper, this);

        Project project = SnabbleUI.getProject();

        checkoutIdCode = findViewById(R.id.checkout_id_code);
        cancel = findViewById(R.id.cancel);
        cancelProgress = findViewById(R.id.cancel_progress);

        helperText = findViewById(R.id.helper_text);
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

        checkout = SnabbleUI.getProject().getCheckout();
        onStateChanged(checkout.getState());
    }

    public void setHelperImage(Bitmap bitmap) {
        if (bitmap != null) {
            helperImage.setImageBitmap(bitmap);
            upArrow.setVisibility(View.VISIBLE);
            helperImage.setVisibility(View.VISIBLE);
            helperText.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.GONE);
        } else {
            upArrow.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            helperText.setVisibility(View.VISIBLE);
            progressIndicator.setVisibility(View.GONE);
        }

        if (currentState == Checkout.State.PAYMENT_PROCESSING) {
            checkoutIdCode.setVisibility(View.GONE);
            helperText.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            upArrow.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.INVISIBLE);
            cancelProgress.setVisibility(View.INVISIBLE);
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

            onStateChanged(checkout.getState());
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
                    checkoutIdCode.setText("snabble:checkoutProcess:" + id);
                }
                break;
            case PAYMENT_PROCESSING:
                checkoutIdCode.setVisibility(View.GONE);
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
        }

        currentState = state;
    }
}
