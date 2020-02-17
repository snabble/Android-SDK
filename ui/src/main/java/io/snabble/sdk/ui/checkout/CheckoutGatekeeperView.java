package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private View message;
    private Checkout.State currentState;
    private TextView helperText;
    private ImageView helperImage;
    private View upArrow;

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
        message = findViewById(R.id.helperText);
        cancel = findViewById(R.id.cancel);
        cancelProgress = findViewById(R.id.cancel_progress);

        helperText = findViewById(R.id.helperText);
        helperImage = findViewById(R.id.helperImage);
        upArrow = findViewById(R.id.arrow);

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
        } else {
            upArrow.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            helperText.setVisibility(View.VISIBLE);
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
                    checkoutIdCode.setText("snabble:checkoutProcess:" + id);
                }
                break;
            case PAYMENT_PROCESSING:
                checkoutIdCode.setVisibility(View.GONE);
                message.setVisibility(View.GONE);
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
}
