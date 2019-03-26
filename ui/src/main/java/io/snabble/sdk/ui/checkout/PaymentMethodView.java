package io.snabble.sdk.ui.checkout;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

class PaymentMethodView extends FrameLayout implements PaymentCredentialsStore.Callback {
    private static Map<PaymentMethod, Integer> icons = new HashMap<>();
    private static Map<PaymentMethod, String> descriptions = new HashMap<>();

    static {
        icons.put(PaymentMethod.CASH, R.drawable.ic_pm_sepa);
        icons.put(PaymentMethod.TELECASH_DIRECT_DEBIT, R.drawable.ic_pm_sepa);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.ic_pm_checkstand);
        icons.put(PaymentMethod.ENCODED_CODES, R.drawable.ic_pm_checkstand);
        icons.put(PaymentMethod.ENCODED_CODES_CSV, R.drawable.ic_pm_checkstand);
    }

    private Checkout checkout;
    private List<Entry> entries;
    private Project project;
    private PaymentCredentialsStore paymentCredentialsStore;
    private RecyclerView recyclerView;

    public PaymentMethodView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentMethodView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_payment_method, this);

        descriptions.clear();

        project = SnabbleUI.getProject();
        checkout = project.getCheckout();
        paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();

        recyclerView = findViewById(R.id.payment_methods);
        recyclerView.setAdapter(new Adapter());

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
    }

    private void update() {
        entries = new ArrayList<>();

        List<PaymentMethod> availablePaymentMethods = Arrays.asList(checkout.getAvailablePaymentMethods());
        for (final PaymentMethod paymentMethod : availablePaymentMethods) {
            if (icons.containsKey(paymentMethod) || descriptions.containsKey(paymentMethod)) {
                Entry e = new Entry();
                e.text = descriptions.get(paymentMethod);
                e.paymentMethod = paymentMethod;

                if(paymentMethod == PaymentMethod.TELECASH_DIRECT_DEBIT) {
                    e.onClickListener = new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showSEPACardInput();
                        }
                    };
                }
                entries.add(e);
            }
        }

        List<PaymentCredentials> paymentCredentials = paymentCredentialsStore.getAll();
        for(PaymentCredentials p : paymentCredentials) {
            if (p.getType() == PaymentCredentials.Type.SEPA) {
                for(Entry e : entries) {
                    if (e.paymentMethod == PaymentMethod.TELECASH_DIRECT_DEBIT) {
                        entries.remove(e);
                        break;
                    }
                }
            }
        }

        for(PaymentCredentials p : paymentCredentials) {
            if (p.getType() == PaymentCredentials.Type.SEPA && availablePaymentMethods.contains(PaymentMethod.TELECASH_DIRECT_DEBIT)) {
                Entry e = new Entry();
                e.paymentMethod = PaymentMethod.TELECASH_DIRECT_DEBIT;
                e.paymentCredentials = p;
                e.text = p.getObfuscatedId();
                entries.add(e);
            }
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void showSEPACardInput() {
        if(Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()
                && KeyguardUtils.isDeviceSecure()) {
            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if(callback != null) {
                callback.showSEPACardInput();
            }
        } else {
            showPaymentNotPossibleDialog();
        }
    }

    private void showSEPALegalInfoIfNeeded(final PaymentMethod paymentMethod, final OnClickListener clickListener) {
        if (paymentMethod != PaymentMethod.TELECASH_DIRECT_DEBIT) {
            clickListener.onClick(null);
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_sepa_legal_info, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        final TextView message = view.findViewById(R.id.message);
        View ok = view.findViewById(R.id.button);
        View close = view.findViewById(R.id.close);

        Resources res = getResources();
        final String shortText = res.getString(R.string.Snabble_Payment_SEPA_legalInfo_short, project.getCheckedInShop().getName());
        final String longText = res.getString(R.string.Snabble_Payment_SEPA_legalInfo_long, project.getCheckedInShop().getName());
        String highlightedString = res.getString(R.string.Snabble_Payment_SEPA_legalInfo_highlight);

        Spannable spannable = new SpannableString(shortText);
        int index = shortText.lastIndexOf(highlightedString);
        int length = highlightedString.length();
        int color = ResourcesCompat.getColor(getResources(), R.color.snabble_primaryColor, null);

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                message.setText(longText);
            }
        }, index, index+length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new ForegroundColorSpan(color), index, index+length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        View button;
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.button);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.image);
        }
    }

    private class Adapter extends RecyclerView.Adapter<PaymentMethodView.ViewHolder> {
        @Override
        public PaymentMethodView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_method, parent, false);
            return new PaymentMethodView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final PaymentMethodView.ViewHolder holder, final int position) {
            final Entry e = entries.get(position);

            Integer drawableResId = icons.get(e.paymentMethod);
            ImageView imageView = holder.image;
            if (drawableResId != null) {
                imageView.setImageResource(drawableResId);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }

            TextView textView = holder.text;
            if (e.text != null) {
                textView.setText(e.text);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }

            if(e.onClickListener != null){
                holder.button.setOnClickListener(e.onClickListener);
            } else {
                holder.button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()
                                && e.paymentMethod.isRequiringCredentials()) {
                            showSEPALegalInfoIfNeeded(e.paymentMethod, new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    final SnabbleUICallback callback = SnabbleUI.getUiCallback();
                                    if (callback != null) {
                                        callback.requestKeyguard(new KeyguardHandler() {
                                            @Override
                                            public void onKeyguardResult(int resultCode) {
                                                if (resultCode == Activity.RESULT_OK) {
                                                    checkout.pay(e.paymentMethod, e.paymentCredentials);
                                                    Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                                                } else {
                                                    callback.goBack();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            checkout.pay(e.paymentMethod, e.paymentCredentials);
                            Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private void registerListeners() {
        paymentCredentialsStore.addCallback(this);
        update();
    }

    private void unregisterListeners() {
        paymentCredentialsStore.removeCallback(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
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
