package io.snabble.sdk.ui.checkout;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import io.snabble.sdk.payment.SEPAPaymentCredentials;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

class PaymentMethodView extends FrameLayout implements PaymentCredentialsStore.Callback {
    private static Map<PaymentMethod, Integer> icons = new HashMap<>();
    private static Map<PaymentMethod, String> descriptions = new HashMap<>();

    static {
        icons.put(PaymentMethod.CASH, R.drawable.ic_sepa);
        //icons.put(PaymentMethod.SEPA, R.drawable.ic_sepa);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.ic_pm_checkstand);
        icons.put(PaymentMethod.ENCODED_CODES, R.drawable.ic_pm_checkstand);
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

        Resources res = getResources();
        descriptions.clear();
        descriptions.put(PaymentMethod.ENCODED_CODES, res.getString(R.string.Snabble_PaymentMethod_encodedCodes));
        descriptions.put(PaymentMethod.QRCODE_POS, res.getString(R.string.Snabble_PaymentMethod_qrCodePOS));

        project = SnabbleUI.getProject();
        checkout = project.getCheckout();
        paymentCredentialsStore = Snabble.getInstance().getUserPreferences().getPaymentCredentialsStore();


        recyclerView = findViewById(R.id.payment_methods);
        recyclerView.setAdapter(new Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        final NestedScrollView scrollView = findViewById(R.id.scroller);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        int dividerColor = ResourcesCompat.getColor(getResources(), R.color.snabble_dividerColor, null);
        dividerItemDecoration.setDrawable(new ColorDrawable(dividerColor));
        recyclerView.addItemDecoration(dividerItemDecoration);

        TextView title = findViewById(R.id.choose_payment_title);

        PriceFormatter priceFormatter = new PriceFormatter(project);
        String totalPriceText = priceFormatter.format(project.getCheckout().getPriceToPay());
        title.setText(getResources().getString(R.string.Snabble_PaymentSelection_howToPay, totalPriceText));

        update();
    }

    private void update() {
        entries = new ArrayList<>();

        List<PaymentMethod> availablePaymentMethods = Arrays.asList(checkout.getAvailablePaymentMethods());
        for (PaymentMethod paymentMethod : availablePaymentMethods) {
            if (icons.containsKey(paymentMethod) || descriptions.containsKey(paymentMethod)) {
                Entry e = new Entry();
                e.text = descriptions.get(paymentMethod);
                e.paymentMethod = paymentMethod;

                if(paymentMethod == PaymentMethod.CASH) {
                    e.onClickListener = new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            SnabbleUICallback callback = SnabbleUI.getUiCallback();
                            if(callback != null) {
                                callback.showSEPACardInput();
                            }
                        }
                    };
                }
                entries.add(e);
            }
        }

        List<PaymentCredentials> paymentCredentials = paymentCredentialsStore.getUserPaymentMethods();
        for(PaymentCredentials p : paymentCredentials) {
            if (p instanceof SEPAPaymentCredentials) {
                for(Entry e : entries) {
                    if (e.paymentMethod == PaymentMethod.CASH) {
                        entries.remove(e);
                        break;
                    }
                }
            }
        }

        for(PaymentCredentials p : paymentCredentials) {
            if (p instanceof SEPAPaymentCredentials && availablePaymentMethods.contains(PaymentMethod.CASH)) {
                Entry e = new Entry();
                e.paymentMethod = PaymentMethod.CASH;
                e.text = p.getObfuscatedId();
                entries.add(e);
            }
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onChanged() {
        update();
    }

    private static class Entry {
        String text;
        PaymentMethod paymentMethod;
        OnClickListener onClickListener;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
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
            if (drawableResId != null) {
                ImageView imageView = holder.image;
                imageView.setImageResource(drawableResId);
                imageView.setVisibility(View.VISIBLE);
            }

            TextView textView = holder.text;
            if (e.text != null) {
                textView.setText(e.text);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }

            if(e.onClickListener != null){
                holder.itemView.setOnClickListener(e.onClickListener);
            } else {
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkout.pay(e.paymentMethod);
                        Telemetry.event(Telemetry.Event.SelectedPaymentMethod, e.paymentMethod);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    public void registerListeners() {
        paymentCredentialsStore.addCallback(this);
        update();
    }

    public void unregisterListeners() {
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
