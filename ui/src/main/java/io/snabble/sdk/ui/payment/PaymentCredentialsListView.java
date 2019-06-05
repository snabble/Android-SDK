package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class PaymentCredentialsListView extends FrameLayout implements PaymentCredentialsStore.Callback {
    private List<Entry> entries = new ArrayList<>();
    private PaymentCredentialsStore paymentCredentialsStore;
    private RecyclerView recyclerView;

    public PaymentCredentialsListView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentCredentialsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentCredentialsListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_payment_credentials_list, this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new PaymentCredentialsListView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        View fab = findViewById(R.id.fab);
        fab.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                if (KeyguardUtils.isDeviceSecure()) {
                    Activity activity = UIUtils.getHostActivity(getContext());
                    if (activity instanceof FragmentActivity) {
                        SelectPaymentMethodFragment dialogFragment = new SelectPaymentMethodFragment();
                        dialogFragment.show(((FragmentActivity) activity).getSupportFragmentManager(), null);
                    } else {
                        throw new RuntimeException("Host activity needs to be a Fragment Activity");
                    }
                } else {
                    new AlertDialog.Builder(getContext())
                            .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                            .setPositiveButton(R.string.Snabble_OK, null)
                            .setCancelable(false)
                            .show();
                }
            }
        });

        entries.clear();

        paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();
        onChanged();
    }

    private static class Entry {
        int drawableRes;
        String text;
        PaymentCredentials paymentCredentials;

        Entry(PaymentCredentials paymentCredentials, int drawableRes, String text) {
            this.paymentCredentials = paymentCredentials;
            this.drawableRes = drawableRes;
            this.text = text;
        }
    }

    private class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        View add;

        EmptyStateViewHolder(View itemView) {
            super(itemView);
            add = itemView.findViewById(R.id.add);
        }
    }

    private class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        TextView validTo;
        ImageView icon;
        View delete;

        EntryViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            validTo = itemView.findViewById(R.id.valid_to);
            icon = itemView.findViewById(R.id.icon);
            delete = itemView.findViewById(R.id.delete);
        }
    }

    @Override
    public void onChanged() {
        entries.clear();

        List<PaymentCredentials> paymentCredentials = paymentCredentialsStore.getAll();

        for(PaymentCredentials pm : paymentCredentials) {
            if(pm.getType() == PaymentCredentials.Type.SEPA) {
                entries.add(new Entry(pm, R.drawable.snabble_ic_sepa_small, pm.getObfuscatedId()));
            } else if (pm.getType() == PaymentCredentials.Type.CREDIT_CARD) {
                PaymentCredentials.Brand ccBrand = pm.getBrand();

                int drawableResId = 0;
                if (ccBrand != null) {
                    switch (ccBrand) {
                        case VISA:
                            drawableResId = R.drawable.snabble_ic_visa;
                            break;
                        case MASTERCARD:
                            drawableResId = R.drawable.snabble_ic_mastercard;
                            break;
                    }
                }

                entries.add(new Entry(pm, drawableResId, pm.getObfuscatedId()));
            }
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final static int TYPE_EMPTYSTATE = 0;
        private final static int TYPE_ENTRY = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == TYPE_EMPTYSTATE) {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_credentials_list_emptystate, parent, false);
                return new EmptyStateViewHolder(v);
            }  else {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_credentials_list_entry, parent, false);
                return new EntryViewHolder(v);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(entries.size() == 0){
                return TYPE_EMPTYSTATE;
            }

            return TYPE_ENTRY;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if(type == TYPE_ENTRY){
                EntryViewHolder vh = (EntryViewHolder)holder;
                final Entry e = entries.get(position);

                if (e.drawableRes != 0) {
                    vh.icon.setImageResource(e.drawableRes);
                    vh.icon.setVisibility(View.VISIBLE);
                } else {
                    vh.icon.setVisibility(View.INVISIBLE);
                }

                String validTo = e.paymentCredentials.getValidTo();

                if (e.paymentCredentials.getValidTo() != null) {
                    // TODO i18n
                    vh.validTo.setText("Expiration date: " + validTo);
                    vh.validTo.setVisibility(View.VISIBLE);
                } else {
                    vh.validTo.setVisibility(View.GONE);
                }

                vh.text.setText(e.text);
                vh.delete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(getContext())
                                .setMessage(R.string.Snabble_Payment_delete_message)
                                .setPositiveButton(R.string.Snabble_Yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        paymentCredentialsStore.remove(e.paymentCredentials);
                                    }
                                })
                                .setNegativeButton(R.string.Snabble_No, null)
                                .create()
                                .show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return Math.max(1, entries.size());
        }
    }

    private void registerListeners() {
        paymentCredentialsStore.addCallback(this);
        onChanged();
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
                public void onActivityResumed(Activity activity) {
                    onChanged();
                }

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
