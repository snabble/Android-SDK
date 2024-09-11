package io.snabble.sdk.ui.payment;

import static io.snabble.sdk.payment.PaymentCredentials.Brand;
import static io.snabble.sdk.payment.PaymentCredentials.Type;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.remotetheme.RemoteThemingExtensionsKt;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class PaymentCredentialsListView extends FrameLayout implements PaymentCredentialsStore.Callback {
    public static final String ARG_PAYMENT_TYPE = "paymentType";
    public static final String ARG_PROJECT_ID = "projectId";

    private final List<Entry> entries = new ArrayList<>();
    private PaymentCredentialsStore paymentCredentialsStore;
    private RecyclerView recyclerView;
    private List<Type> types;
    private Project project;
    private View emptyState;

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
        if (isInEditMode()) return;
        paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new PaymentCredentialsListView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        FloatingActionButton fab = findViewById(R.id.fab);
        final Project currentProject = Snabble.getInstance().getCheckedInProject().getLatestValue();
        final int primaryColor = RemoteThemingExtensionsKt.getPrimaryColorForProject(getContext(), currentProject);
        final int onPrimaryColor = RemoteThemingExtensionsKt.getOnPrimaryColorForProject(getContext(), currentProject);
        fab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        fab.setImageTintList(ColorStateList.valueOf(onPrimaryColor));

        fab.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                if (KeyguardUtils.isDeviceSecure()) {
                    Activity activity = UIUtils.getHostActivity(getContext());
                    if (activity instanceof FragmentActivity) {
                        SelectPaymentMethodFragment dialogFragment = new SelectPaymentMethodFragment();
                        Bundle bundle = new Bundle();

                        Project p = project;
                        if (p == null) {
                            p = Snabble.getInstance().getCheckedInProject().getValue();
                        }

                        if (p == null) {
                            throw new IllegalStateException("Cannot get current project. Did you forget to set the projectId to the PaymentCredentialsListFragment or PaymentCredentialsListView?");
                        }

                        bundle.putString(SelectPaymentMethodFragment.ARG_PROJECT_ID, p.getId());
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(((FragmentActivity) activity).getSupportFragmentManager(), null);
                    } else {
                        throw new RuntimeException("Host activity must be a Fragment Activity");
                    }
                } else {

                    final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                            .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                            .setPositiveButton(R.string.Snabble_ok, null)
                            .setCancelable(false)
                            .create();

                    alertDialog.setOnShowListener(dialog -> {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryColor);
                    });

                    alertDialog.show();
                }
            }
        });

        emptyState = findViewById(R.id.empty_state);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void show(List<Type> types, Project project) {
        this.types = types;
        this.project = project;

        entries.clear();
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

    private static class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        View add;

        EmptyStateViewHolder(View itemView) {
            super(itemView);
            add = itemView.findViewById(R.id.add);
        }
    }

    private static class EntryViewHolder extends RecyclerView.ViewHolder {
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

        for (PaymentCredentials pm : paymentCredentials) {
            boolean sameProjectOrNull = true;
            if (project != null) {
                sameProjectOrNull = project.getId().equals(pm.getProjectId());
            }

            if (pm.getProjectId() == null && (pm.getType() == Type.SEPA || pm.getType() == Type.PAYONE_SEPA)) {
                sameProjectOrNull = true;
            }

            boolean sameTypeOrNull = true;
            if (types != null) {
                if (project != null) {
                    sameTypeOrNull = project.getAvailablePaymentMethods().contains(pm.getPaymentMethod());
                } else {
                    sameTypeOrNull = types.contains(pm.getType());
                }
            }
            if (pm.isAvailableInCurrentApp() && sameTypeOrNull && sameProjectOrNull) {
                switch (pm.getType()) {
                    case SEPA:
                    case PAYONE_SEPA:
                        entries.add(new Entry(pm, R.drawable.snabble_ic_payment_select_sepa, pm.getObfuscatedId()));
                        break;
                    case CREDIT_CARD_PSD2:
                    case DATATRANS:
                    case DATATRANS_CREDITCARD:
                    case PAYONE_CREDITCARD:
                        entries.add(new Entry(pm, getDrawableForBrand(pm.getBrand()), pm.getObfuscatedId()));
                        break;
                    case GIROPAY:
                        entries.add(new Entry(pm, R.drawable.snabble_ic_payment_giropay, pm.getObfuscatedId()));
                        break;
                    case TEGUT_EMPLOYEE_CARD:
                        entries.add(new Entry(pm, R.drawable.snabble_ic_payment_select_tegut, pm.getObfuscatedId()));
                        break;
                    case EXTERNAL_BILLING:
                        entries.add(new Entry(pm, R.drawable.ic_snabble_external_billing, pm.getObfuscatedId()));
                        break;
                }
            }
        }

        if (project != null && project.getGooglePayHelper() != null) {
            entries.add(0, new Entry(null, R.drawable.snabble_ic_payment_select_gpay, "Google Pay"));
        }

        if (entries.size() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private int getDrawableForBrand(Brand brand) {
        int drawableResId = 0;
        if (brand != null) {
            switch (brand) {
                case VISA:
                    drawableResId = R.drawable.snabble_ic_payment_select_visa;
                    break;
                case MASTERCARD:
                    drawableResId = R.drawable.snabble_ic_payment_select_mastercard;
                    break;
                case AMEX:
                    drawableResId = R.drawable.snabble_ic_payment_select_amex;
                    break;
                case POST_FINANCE_CARD:
                    drawableResId = R.drawable.snabble_ic_payment_select_postfinance;
                    break;
                case TWINT:
                    drawableResId = R.drawable.snabble_ic_payment_select_twint;
                    break;
            }
        }

        return drawableResId;
    }

    private class Adapter extends RecyclerView.Adapter<EntryViewHolder> {
        @NonNull
        @Override
        public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_credentials_list_entry, parent, false);
            return new EntryViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final EntryViewHolder vh, final int position) {
            final Entry e = entries.get(position);

            if (e.drawableRes != 0) {
                vh.icon.setImageResource(e.drawableRes);
                vh.icon.setVisibility(View.VISIBLE);
            } else {
                vh.icon.setVisibility(View.INVISIBLE);
            }

            if (e.paymentCredentials != null && e.paymentCredentials.getValidTo() != 0) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/yyyy");
                String validTo = simpleDateFormat.format(e.paymentCredentials.getValidTo());

                vh.validTo.setText(getResources().getString(R.string.Snabble_Payment_CreditCard_expireDate, validTo));
                vh.validTo.setVisibility(View.VISIBLE);
            } else {
                vh.validTo.setVisibility(View.GONE);
            }

            vh.text.setText(e.text);

            if (e.paymentCredentials != null) {
                vh.delete.setOnClickListener(view -> {
                    final int primaryColor = RemoteThemingExtensionsKt.getPrimaryColorForProject(
                            getContext(),
                            Snabble.getInstance().getCheckedInProject().getLatestValue()
                    );

                    final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                            .setMessage(R.string.Snabble_Payment_Delete_message)
                            .setPositiveButton(R.string.Snabble_yes, (dialog, which) -> paymentCredentialsStore.remove(e.paymentCredentials))
                            .setNegativeButton(R.string.Snabble_no, null)
                            .create();

                    alertDialog.setOnShowListener(dialog -> {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryColor);
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(primaryColor);
                    });

                    alertDialog.show();

                    Telemetry.event(Telemetry.Event.PaymentMethodDeleted, e.paymentCredentials.getType());
                });
            } else {
                vh.delete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
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

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
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
