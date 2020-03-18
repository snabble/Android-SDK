package io.snabble.sdk.ui.checkout;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.UserPreferences;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class PaymentSelectionView extends FrameLayout implements PaymentCredentialsStore.Callback,
        Checkout.OnCheckoutStateChangedListener {
    private static Map<PaymentMethod, Integer> icons = new HashMap<>();

    static {
        icons.put(PaymentMethod.DE_DIRECT_DEBIT, R.drawable.snabble_ic_pm_sepa);
        icons.put(PaymentMethod.VISA, R.drawable.snabble_ic_pm_visa);
        icons.put(PaymentMethod.MASTERCARD, R.drawable.snabble_ic_pm_mastercard);
        icons.put(PaymentMethod.AMEX, R.drawable.snabble_ic_pm_amex);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.snabble_ic_pm_checkstand);
        icons.put(PaymentMethod.QRCODE_OFFLINE, R.drawable.snabble_ic_pm_checkstand);
        icons.put(PaymentMethod.TEGUT_EMPLOYEE_CARD, R.drawable.snabble_ic_pm_tegut);
        icons.put(PaymentMethod.GATEKEEPER_TERMINAL, R.drawable.snabble_ic_pm_sco);
    }

    private Checkout checkout;
    private List<Entry> entries;
    private Project project;
    private PaymentCredentialsStore paymentCredentialsStore;
    private RecyclerView recyclerView;
    private Adapter recyclerViewAdapter;
    private UserPreferences userPreferences;
    private DelayedProgressDialog progressDialog;
    private Checkout.State currentState;
    private Map<PaymentMethod, String> descriptions = new HashMap<>();

    public PaymentSelectionView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        Snabble.getInstance()._setCurrentActivity(UIUtils.getHostActivity(getContext()));

        inflate(getContext(), R.layout.snabble_view_payment_selection, this);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                checkout.abort();
                return true;
            }
            return false;
        });

        descriptions.put(PaymentMethod.GATEKEEPER_TERMINAL, getResources().getString(R.string.Snabble_Payment_payAtSCO));

        project = SnabbleUI.getProject();
        checkout = project.getCheckout();
        paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();
        userPreferences = Snabble.getInstance().getUserPreferences();

        recyclerView = findViewById(R.id.payment_methods);
        recyclerViewAdapter = new Adapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(null);

        TextView title = findViewById(R.id.choose_payment_title);

        PriceFormatter priceFormatter = project.getPriceFormatter();
        String totalPriceText = priceFormatter.format(project.getCheckout().getPriceToPay());
        String titleText = getResources().getString(R.string.Snabble_PaymentSelection_payNow, totalPriceText);

        if (SnabbleUI.getActionBar() != null) {
            title.setVisibility(View.GONE);
            SnabbleUI.getActionBar().setTitle(titleText);
        } else {
            title.setText(titleText);
        }

        update();

        setKeepScreenOn(true);

        checkout = SnabbleUI.getProject().getCheckout();
        onStateChanged(checkout.getState());
    }

    private void update() {
        entries = new ArrayList<>();

        PaymentMethod[] availablePaymentMethods = checkout.getAvailablePaymentMethods();
        for (final PaymentMethod paymentMethod : availablePaymentMethods) {
            if (icons.containsKey(paymentMethod)) {
                List<PaymentCredentials> pcList = getPaymentCredentials(paymentMethod);
                if (pcList.size() > 0) {
                    for (PaymentCredentials pc : pcList) {
                        final Entry e = new Entry();
                        e.text = descriptions.get(paymentMethod);
                        e.paymentMethod = paymentMethod;
                        e.paymentCredentials = pc;
                        e.text = pc.getObfuscatedId();
                        e.desaturated = false;

                        e.onClickListener = v -> showSEPALegalInfoIfNeeded(e.paymentMethod, v1 -> {
                            final SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                            if (callback == null) {
                                return;
                            }

                            if (userPreferences.isRequiringKeyguardAuthenticationForPayment()
                                    && e.paymentMethod.isRequiringCredentials()) {
                                Keyguard.unlock(UIUtils.getHostFragmentActivity(getContext()), new Keyguard.Callback() {
                                    @Override
                                    public void success() {
                                        checkout.pay(e.paymentMethod, e.paymentCredentials);
                                        Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                                    }

                                    @Override
                                    public void error() {
                                        callback.execute(SnabbleUI.Action.GO_BACK, null);
                                    }
                                });
                            } else {
                                checkout.pay(e.paymentMethod, e.paymentCredentials);
                                Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                            }
                        });

                        entries.add(e);
                    }
                } else if (!paymentMethod.isShowOnlyIfCredentialsArePresent()) {
                    final Entry e = new Entry();
                    e.text = descriptions.get(paymentMethod);
                    e.paymentMethod = paymentMethod;
                    e.desaturated = false;

                    switch (paymentMethod) {
                        case DE_DIRECT_DEBIT:
                            e.onClickListener = v -> showSEPACardInput();
                            e.desaturated = true;
                            break;
                        case MASTERCARD:
                        case VISA:
                            e.onClickListener = v -> showCreditCardInput();
                            e.desaturated = true;
                            break;
                        default:
                            e.onClickListener = v -> {
                                checkout.pay(e.paymentMethod, e.paymentCredentials);
                                Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                            };
                            break;
                    }

                    entries.add(e);
                }
            }
        }

        // TODO move to Checkout
        filterPaymentMethods();

        recyclerViewAdapter.notifyDataSetChanged();
    }

    private void filterPaymentMethods() {
        boolean hasPaymentMethodWithCredentials = false;
        boolean hasGatekeeperTerminal = false;
        for (Entry e : entries) {
            if (e.paymentCredentials != null) {
                hasPaymentMethodWithCredentials = true;
            }

            if (e.paymentMethod == PaymentMethod.GATEKEEPER_TERMINAL) {
                hasGatekeeperTerminal = true;
            }
        }

        if (hasPaymentMethodWithCredentials || hasGatekeeperTerminal) {
            ArrayList<Entry> removals = new ArrayList<>();
            for (Entry e : entries) {
                if (e.paymentCredentials == null && e.paymentMethod.isRequiringCredentials()) {
                    removals.add(e);
                }
            }

            for (Entry e : removals) {
                entries.remove(e);
            }
        }
    }

    private List<PaymentCredentials> getPaymentCredentials(PaymentMethod pm) {
        ArrayList<PaymentCredentials> list = new ArrayList<>();

        List<PaymentCredentials> paymentCredentials = paymentCredentialsStore.getAll();
        for (PaymentCredentials pc : paymentCredentials) {
            if (pc.getType() == getType(pm) && pc.getBrand() == getBrand(pm)
                    && Snabble.getInstance().getConfig().appId.equals(pc.getAppId())) {
                list.add(pc);
            }
        }

        return list;
    }

    private PaymentCredentials.Type getType(PaymentMethod pm) {
        switch (pm) {
            case VISA:
            case MASTERCARD:
            case AMEX:
                return PaymentCredentials.Type.CREDIT_CARD;
            case DE_DIRECT_DEBIT:
                return PaymentCredentials.Type.SEPA;
            case TEGUT_EMPLOYEE_CARD:
                return PaymentCredentials.Type.TEGUT_EMPLOYEE_CARD;
            default:
                return null;
        }
    }

    private PaymentCredentials.Brand getBrand(PaymentMethod pm) {
        switch (pm) {
            case VISA:
                return PaymentCredentials.Brand.VISA;
            case MASTERCARD:
                return PaymentCredentials.Brand.MASTERCARD;
            case AMEX:
                return PaymentCredentials.Brand.AMEX;
            default:
                return PaymentCredentials.Brand.UNKNOWN;
        }
    }

    private void showSEPACardInput() {
        if (userPreferences.isRequiringKeyguardAuthenticationForPayment()) {
            if (KeyguardUtils.isDeviceSecure()) {
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null);
                }
            } else {
                showPaymentNotPossibleDialog();
            }
        } else {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null);
            }
        }
    }

    private void showCreditCardInput() {
        if (userPreferences.isRequiringKeyguardAuthenticationForPayment()) {
            if (KeyguardUtils.isDeviceSecure()) {
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
                }
            } else {
                showPaymentNotPossibleDialog();
            }
        } else {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
            }
        }
    }

    private void showSEPALegalInfoIfNeeded(final PaymentMethod paymentMethod, final OnClickListener clickListener) {
        if (paymentMethod != PaymentMethod.DE_DIRECT_DEBIT) {
            clickListener.onClick(null);
            return;
        }

        String shortText = project.getText("sepaMandateShort");
        final String longText = project.getText("sepaMandate");

        if (shortText == null || longText == null) {
            clickListener.onClick(null);
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.snabble_dialog_sepa_legal_info, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        final TextView message = view.findViewById(R.id.helper_text);
        View ok = view.findViewById(R.id.button);
        View close = view.findViewById(R.id.close);

        int startIndex = shortText.indexOf('*');
        int endIndex = shortText.lastIndexOf('*') - 1;
        shortText = shortText.replace("*", "");
        Spannable spannable = new SpannableString(shortText);

        if (startIndex != -1 && endIndex != -1) {
            int color = UIUtils.getColorByAttribute(getContext(), R.attr.colorPrimary);

            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    message.setText(longText);
                }
            }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannable.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        message.setText(spannable);
        message.setMovementMethod(LinkMovementMethod.getInstance());

        close.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
            }
        });

        ok.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
                clickListener.onClick(null);
            }
        });

        alertDialog.show();
        alertDialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void showPaymentNotPossibleDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                .setPositiveButton(R.string.Snabble_OK, null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onChanged() {
        update();
    }

    private static class Entry {
        String text;
        PaymentCredentials paymentCredentials;
        PaymentMethod paymentMethod;
        OnClickListener onClickListener;
        boolean desaturated;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        View card;
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);

            card = itemView.findViewById(R.id.card);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.helper_image);
        }
    }

    private class Adapter extends RecyclerView.Adapter<PaymentSelectionView.ViewHolder> {
        @Override
        public PaymentSelectionView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_method, parent, false);
            return new PaymentSelectionView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final PaymentSelectionView.ViewHolder holder, final int position) {
            final Entry e = entries.get(position);

            Integer drawableResId = icons.get(e.paymentMethod);
            ImageView imageView = holder.image;
            if (drawableResId != null) {
                imageView.setImageResource(drawableResId);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }

            if (e.desaturated) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                imageView.setColorFilter(filter);
            } else {
                imageView.clearColorFilter();
            }

            TextView textView = holder.text;
            if (e.text != null) {
                textView.setText(e.text);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }

            holder.card.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
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
            case PAYMENT_PROCESSING:
            case WAIT_FOR_APPROVAL:
                CheckoutHelper.displayPaymentView(checkout);
                break;
            case PAYMENT_APPROVED:
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.SHOW_PAYMENT_SUCCESS, null);
                }
                break;
            case CONNECTION_ERROR:
                UIUtils.snackbar(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show();
                break;
            case NONE:
                break;
        }

        currentState = state;
    }


    private void registerListeners() {
        checkout.addOnCheckoutStateChangedListener(this);
        paymentCredentialsStore.addCallback(this);
        update();
    }

    private void unregisterListeners() {
        checkout.removeOnCheckoutStateChangedListener(this);
        paymentCredentialsStore.removeCallback(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
        onStateChanged(checkout.getState());
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
                public void onActivityPaused(Activity activity) {
                    super.onActivityPaused(activity);

                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
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
