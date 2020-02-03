package io.snabble.sdk.ui.checkout;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class CheckoutView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private CoordinatorLayout coordinatorLayout;
    private ViewAnimator viewAnimator;
    private Checkout checkout;
    private DelayedProgressDialog progressDialog;
    private Checkout.State currentState;
    private View successView;
    private View failureView;
    private OnCheckoutScreenChangedListener onCheckoutScreenChangedListener;

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
        inflate(getContext(), R.layout.snabble_view_checkout, this);

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        viewAnimator = findViewById(R.id.view_animator);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    checkout.abort();
                    return true;
                }
                return false;
            }
        });

        setKeepScreenOn(true);

        checkout = SnabbleUI.getProject().getCheckout();
        onStateChanged(checkout.getState());
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == currentState) {
            return;
        }

        if (state == Checkout.State.VERIFYING_PAYMENT_METHOD) {
            progressDialog.showAfterDelay(500);
        } else {
            progressDialog.dismiss();
        }

        switch (state) {
            case REQUEST_PAYMENT_METHOD:
                PaymentSelectionView paymentSelectionView = new PaymentSelectionView(getContext());
                displayView(paymentSelectionView);
                break;
            case PAYMENT_PROCESSING:
            case WAIT_FOR_APPROVAL:
                displayPaymentView();
                break;
            case PAYMENT_APPROVED:
                if (currentState == Checkout.State.PAYMENT_APPROVED) {
                    break;
                }

                if (successView != null) {
                    displayView(successView);
                } else {
                    displayView(new CheckoutDoneView(getContext()));
                }

                Telemetry.event(Telemetry.Event.CheckoutSuccessful);
                break;
            case PAYMENT_ABORT_FAILED:
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
            case REQUEST_ADD_PAYMENT_ORIGIN:
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.Snabble_Payment_addPaymentOrigin)
                        .setPositiveButton(R.string.Snabble_Yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addPaymentOrigin();
                            }
                        })
                        .setNegativeButton(R.string.Snabble_No, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkout.continuePaymentProcess();
                            }
                        })
                        .create()
                        .show();
                break;
            case PAYMENT_ABORTED:
            case DENIED_BY_PAYMENT_PROVIDER:
            case DENIED_BY_SUPERVISOR:
                if (failureView != null) {
                    displayView(failureView);
                } else {
                    displayView(new CheckoutAbortedView(getContext()));
                }
                break;
            case CONNECTION_ERROR:
                UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show();
                break;
            case NONE:
                break;
        }

        currentState = state;
    }

    private void addPaymentOrigin() {
        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            Keyguard.unlock(UIUtils.getHostFragmentActivity(getContext()), new Keyguard.Callback() {
                @Override
                public void success() {
                    addPaymentCredentials();
                    checkout.continuePaymentProcess();
                }

                @Override
                public void error() {

                }
            });
        } else {
            addPaymentCredentials();
            checkout.continuePaymentProcess();
        }
    }

    public void addPaymentCredentials() {
        Checkout.PaymentOrigin paymentOrigin = checkout.getPaymentOrigin();

        if (paymentOrigin != null) {
            final PaymentCredentials pc = PaymentCredentials.fromSEPA(
                    paymentOrigin.name,
                    paymentOrigin.iban);

            if (pc == null) {
                Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                        .show();
            } else {
                Snabble.getInstance().getPaymentCredentialsStore().add(pc);
                Telemetry.event(Telemetry.Event.PaymentMethodAdded, pc.getType().name());

                checkout.continuePaymentProcess();
            }
        }
    }

    private void displayPaymentView() {
        switch (checkout.getSelectedPaymentMethod()) {
            case TEGUT_EMPLOYEE_CARD:
            case DE_DIRECT_DEBIT:
            case VISA:
            case MASTERCARD:
                displayView(new CheckoutStatusView(getContext()));
                break;
            case GATEKEEPER_TERMINAL:
                displayView(new CheckoutGatekeeperView(getContext()));
                break;
            case QRCODE_POS:
                CheckoutQRCodePOSView checkoutQRCodePOSView = new CheckoutQRCodePOSView(getContext());
                // checkoutQRCodePOSView.setQRCodeText(checkout.getQRCodePOSContent());
                displayView(checkoutQRCodePOSView);
                break;
            case QRCODE_OFFLINE:
                displayView(new CheckoutEncodedCodesView(getContext()));
                break;
        }
    }

    public void displayView(View view) {
        viewAnimator.removeAllViews();
        viewAnimator.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (onCheckoutScreenChangedListener != null) {
            onCheckoutScreenChangedListener.onCheckoutScreenChanged(view);
        }
    }

    /** Sets the view to be shown after a successful checkout (for online methods) **/
    public void setSuccessView(View view) {
        this.successView = view;

        View v = viewAnimator.getCurrentView();
        if (v instanceof CheckoutDoneView) {
            displayView(successView);
        }
    }

    /** Sets the view to be shown after a failed checkout (for online methods) **/
    public void setFailureView(View view) {
        this.failureView = view;

        View v = viewAnimator.getCurrentView();
        if (v instanceof CheckoutAbortedView) {
            displayView(failureView);
        }
    }

    private void registerListeners() {
        checkout.addOnCheckoutStateChangedListener(this);
        onStateChanged(checkout.getState());
    }

    private void unregisterListeners() {
        checkout.removeOnCheckoutStateChangedListener(this);
        progressDialog.dismiss();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();

        if (checkout.getState() == Checkout.State.NONE) {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.GO_BACK, null);
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        unregisterListeners();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }
            };

    public interface OnCheckoutScreenChangedListener {
        void onCheckoutScreenChanged(View view);
    }

    public void setOnCheckoutScreenChangedListener(OnCheckoutScreenChangedListener onCheckoutScreenChangedListener) {
        this.onCheckoutScreenChangedListener = onCheckoutScreenChangedListener;
    }
}
