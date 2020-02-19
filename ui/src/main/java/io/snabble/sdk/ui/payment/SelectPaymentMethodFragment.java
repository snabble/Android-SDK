package io.snabble.sdk.ui.payment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.OneShotClickListener;

public class SelectPaymentMethodFragment extends BottomSheetDialogFragment {
    private List<Entry> entries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.snabble_view_payment_credentials_select, container, false);

        entries = new ArrayList<>();
        entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_sepa_small,
                "SEPA", getUsableAtText(PaymentMethod.DE_DIRECT_DEBIT), new OneShotClickListener() {
            @Override
            public void click() {
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null);
                }

                dismissAllowingStateLoss();
            }
        }));

        // Credit card payments are only supported on API 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_creditcard,
                    getString(R.string.Snabble_Payment_CreditCard),
                    getUsableAtText(PaymentMethod.VISA, PaymentMethod.MASTERCARD), new OneShotClickListener() {
                @Override
                public void click() {
                    SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                    if (callback != null) {
                        callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
                    }

                    dismissAllowingStateLoss();
                }
            }));
        }

        RecyclerView recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new SelectPaymentMethodFragment.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        return v;
    }

    private String getUsableAtText(PaymentMethod...paymentMethods) {
        StringBuilder sb = new StringBuilder();

        int count = 0;
        for (Project project : Snabble.getInstance().getProjects()) {
            List<PaymentMethod> availablePaymentMethods = Arrays.asList(project.getAvailablePaymentMethods());
            for (PaymentMethod pm : paymentMethods) {
                if (availablePaymentMethods.contains(pm)) {
                    if (count > 0 ) {
                        sb.append(", ");
                    }

                    sb.append(project.getName());
                    count++;
                    break;
                }
            }
        }

        if (sb.length() == 0) {
            sb.append("TEST");
        }

        return getResources().getString(R.string.Snabble_Payment_usableAt, sb.toString());
    }

    private static class Entry {
        int drawableRes;
        String text;
        String usableAt;
        View.OnClickListener onClickListener;

        Entry(int drawableRes, String text, String usableAt, View.OnClickListener onClickListener) {
            this.drawableRes = drawableRes;
            this.text = text;
            this.usableAt = usableAt;
            this.onClickListener = onClickListener;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView usableAt;
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            usableAt = itemView.findViewById(R.id.usable_at);
            image = itemView.findViewById(R.id.helper_image);
        }
    }

    private class Adapter extends RecyclerView.Adapter<SelectPaymentMethodFragment.ViewHolder> {
        @Override
        public SelectPaymentMethodFragment.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_credentials_select, parent, false);
            return new SelectPaymentMethodFragment.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final SelectPaymentMethodFragment.ViewHolder holder, final int position) {
            SelectPaymentMethodFragment.Entry e = entries.get(position);

            if (e.drawableRes != 0) {
                holder.image.setImageResource(e.drawableRes);
            }

            holder.text.setText(e.text);
            holder.usableAt.setText(e.usableAt);
            holder.itemView.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}

