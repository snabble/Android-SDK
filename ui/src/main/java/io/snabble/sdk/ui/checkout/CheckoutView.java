package io.snabble.sdk.ui.checkout;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class CheckoutView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private CoordinatorLayout coordinatorLayout;
    private ViewAnimator viewAnimator;
    private Checkout checkout;
    private DelayedProgressDialog progressDialog;

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
                    checkout.cancel();
                    return true;
                }
                return false;
            }
        });

        checkout = SnabbleUI.getProject().getCheckout();
        onStateChanged(checkout.getState());
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.VERIFYING_PAYMENT_METHOD) {
            progressDialog.showAfterDelay(500);
        } else {
            progressDialog.dismiss();
        }

        switch (state) {
            case REQUEST_PAYMENT_METHOD:
                PaymentMethodView paymentMethodView = new PaymentMethodView(getContext());
                displayView(paymentMethodView);
                break;
            case WAIT_FOR_APPROVAL:
                displayPaymentView();
                break;
            case PAYMENT_APPROVED:
                if (!checkout.getSelectedPaymentMethod().isOfflineMethod()) {
                    displayView(new CheckoutDoneView(getContext()));
                }
                break;
            case PAYMENT_ABORTED:
            case DENIED_BY_PAYMENT_PROVIDER:
            case DENIED_BY_SUPERVISOR:
                displayView(new CheckoutAbortedView(getContext()));
                break;
            case CONNECTION_ERROR:
                UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show();
                break;
            case NONE:

                break;
        }
    }

    private void displayPaymentView() {
        Project project = SnabbleUI.getProject();
        switch (checkout.getSelectedPaymentMethod()) {
            case DE_DIRECT_DEBIT:
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
            case ENCODED_CODES_CSV:
                displayView(new CheckoutEncodedCodesView(getContext(),
                        new EncodedCodesOptions.Builder(project)
                        .prefix("snabble;\n")
                        .separator("\n")
                        .suffix("")
                        .repeatCodes(false)
                        .countSeparator(";")
                        .maxCodes(100)
                        .build()));
                break;
            case ENCODED_CODES_IKEA:
                EncodedCodesOptions options = project.getEncodedCodesOptions();
                int maxCodes = 45;
                if (options != null) {
                    maxCodes = options.maxCodes;
                }

                String prefix = "9100003\u001d100{qrCodeCount}";
                if (project.getCustomerCardId() != null) {
                    prefix += "\u001d92" + project.getCustomerCardId();
                }
                prefix += "\u001d240";

                displayView(new CheckoutEncodedCodesView(getContext(),
                        new EncodedCodesOptions.Builder(project)
                        .prefix(prefix)
                        .separator("\u001d240")
                        .suffix("")
                        .maxCodes(maxCodes)
                        .build()));
                break;
        }
    }

    public void displayView(View view) {
        viewAnimator.removeAllViews();
        viewAnimator.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void registerListeners() {
        checkout.addOnCheckoutStateChangedListener(this);
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
            Activity activity = UIUtils.getHostActivity(getContext());
            if (activity != null) {
                activity.onBackPressed();
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
}
