package io.snabble.sdk.ui.payment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.OneShotClickListener;

public class SelectPaymentMethodFragment extends BottomSheetDialogFragment {
    private List<Entry> entries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.snabble_view_payment_credentials_select, container, false);

        entries = new ArrayList<>();
        entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_sepa_small, "SEPA", new OneShotClickListener() {
            @Override
            public void click() {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showSEPACardInput();
                }

                dismissAllowingStateLoss();
            }
        }));

        // Credit card payments are only supported on API 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_visa, "VISA", new OneShotClickListener() {
                @Override
                public void click() {
                    SnabbleUICallback callback = SnabbleUI.getUiCallback();
                    if (callback != null) {
                        callback.showCreditCardInput();
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

    private static class Entry {
        int drawableRes;
        String text;
        View.OnClickListener onClickListener;

        Entry(int drawableRes, String text, View.OnClickListener onClickListener) {
            this.drawableRes = drawableRes;
            this.text = text;
            this.onClickListener = onClickListener;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
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

            holder.text.setCompoundDrawablesWithIntrinsicBounds(e.drawableRes, 0, 0, 0);
            holder.text.setText(e.text);
            holder.itemView.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}

