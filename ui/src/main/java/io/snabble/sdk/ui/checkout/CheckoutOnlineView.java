package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;

public class CheckoutOnlineView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private Checkout checkout;
    private BarcodeView checkoutIdCode;
    private View cancel;
    private View cancelProgress;
    private View message;
    private Checkout.State currentState;

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

        checkoutIdCode = findViewById(R.id.checkout_id_code);
        message = findViewById(R.id.message);
        cancel = findViewById(R.id.cancel);
        cancelProgress = findViewById(R.id.cancel_progress);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkout.abort();
                cancelProgress.setVisibility(View.VISIBLE);
                cancel.setEnabled(false);
            }
        });

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Payment_confirm);
        }

        TextView checkoutId = findViewById(R.id.checkout_id);
        String id = SnabbleUI.getProject().getCheckout().getId();
        if (id != null && id.length() >= 4) {
            String text = getResources().getString(R.string.Snabble_Checkout_ID);
            checkoutId.setText(String.format("%s: %s", text, id.substring(id.length() - 4)));
        } else {
            checkoutId.setVisibility(View.GONE);
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
                        .setPositiveButton(R.string.Snabble_OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                checkout.resume();
                            }
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
