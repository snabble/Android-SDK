package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telecom.Call;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.payment.SEPACard;
import io.snabble.sdk.payment.UserPaymentMethod;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class UserPaymentMethodListView extends FrameLayout implements PaymentCredentialsStore.Callback {
    private List<Entry> entries = new ArrayList<>();
    private PaymentCredentialsStore paymentCredentialsStore;
    private RecyclerView recyclerView;

    public UserPaymentMethodListView(Context context) {
        super(context);
        inflateView();
    }

    public UserPaymentMethodListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public UserPaymentMethodListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_userpaymentmethod_list, this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new UserPaymentMethodListView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        int dividerColor = ResourcesCompat.getColor(getResources(), R.color.snabble_dividerColor, null);
        dividerItemDecoration.setDrawable(new ColorDrawable(dividerColor));
        recyclerView.addItemDecoration(dividerItemDecoration);

        entries.clear();

        SnabbleSdk snabbleSdk = SnabbleUI.getSdkInstance();
        paymentCredentialsStore = snabbleSdk.getUserPreferences().getPaymentCredentialsStore();
        onChanged();
    }

    private static class Entry {
        int drawableRes;
        String text;
        UserPaymentMethod userPaymentMethod;

        Entry(UserPaymentMethod userPaymentMethod, int drawableRes, String text) {
            this.userPaymentMethod = userPaymentMethod;
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

    private class AddViewHolder extends RecyclerView.ViewHolder {
        View add;

        AddViewHolder(View itemView) {
            super(itemView);
            add = itemView.findViewById(R.id.add);
        }
    }

    private class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView icon;
        View delete;

        EntryViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
            delete = itemView.findViewById(R.id.delete);
        }
    }

    @Override
    public void onChanged() {
        entries.clear();

        List<UserPaymentMethod> userPaymentMethods = paymentCredentialsStore.getUserPaymentMethods();

        for(UserPaymentMethod pm : userPaymentMethods) {
            if(pm instanceof SEPACard) {
                SEPACard sepaCard = (SEPACard) pm;
                entries.add(new Entry(pm, R.drawable.ic_sepa_small, sepaCard.getObfuscatedIBAN()));
            }
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final static int TYPE_EMPTYSTATE = 0;
        private final static int TYPE_ENTRY = 1;
        private final static int TYPE_ADD = 2;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == TYPE_EMPTYSTATE) {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.item_userpaymentmethod_list_emptystate, parent, false);
                return new EmptyStateViewHolder(v);
            } else if(viewType == TYPE_ADD) {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.item_userpaymentmethod_list_add, parent, false);
                return new AddViewHolder(v);
            } else {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.item_userpaymentmethod_list_entry, parent, false);
                return new EntryViewHolder(v);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(entries.size() == 0){
                return TYPE_EMPTYSTATE;
            }

            if(position >= entries.size()) {
                return TYPE_ADD;
            }

            return TYPE_ENTRY;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if(type == TYPE_EMPTYSTATE) {
                EmptyStateViewHolder vh = (EmptyStateViewHolder)holder;
                vh.add.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SnabbleUICallback callback = SnabbleUI.getUiCallback();
                        if (callback != null) {
                            callback.showUserPaymentMethodSelect();
                        }
                    }
                });
            } else if(type == TYPE_ADD) {
                AddViewHolder vh = (AddViewHolder)holder;
                vh.add.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SnabbleUICallback callback = SnabbleUI.getUiCallback();
                        if (callback != null) {
                            callback.showUserPaymentMethodSelect();
                        }
                    }
                });
            } else if(type == TYPE_ENTRY){
                EntryViewHolder vh = (EntryViewHolder)holder;
                final Entry e = entries.get(position);

                vh.icon.setImageResource(e.drawableRes);
                vh.text.setText(e.text);
                vh.delete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(getContext())
                                .setMessage(R.string.Snabble_Payment_delete_message)
                                .setPositiveButton(R.string.Snabble_Yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        paymentCredentialsStore.remove(e.userPaymentMethod);
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
            return entries.size() + 1;
        }
    }

    public void registerListeners() {
        paymentCredentialsStore.addCallback(this);
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
