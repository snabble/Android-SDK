package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class UserPaymentMethodSelectView extends FrameLayout {
    private List<Entry> entries;

    public UserPaymentMethodSelectView(Context context) {
        super(context);
        inflateView();
    }

    public UserPaymentMethodSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public UserPaymentMethodSelectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_userpaymentmethod_select, this);

        entries = new ArrayList<>();
        entries.add(new Entry(R.drawable.ic_sepa_small, "SEPA", new OnClickListener() {
            @Override
            public void onClick(View view) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showSEPACardInput();
                }
            }
        }));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new UserPaymentMethodSelectView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        int dividerColor = ResourcesCompat.getColor(getResources(), R.color.snabble_dividerColor, null);
        dividerItemDecoration.setDrawable(new ColorDrawable(dividerColor));
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private static class Entry {
        int drawableRes;
        String text;
        OnClickListener onClickListener;

        Entry(int drawableRes, String text, OnClickListener onClickListener) {
            this.drawableRes = drawableRes;
            this.text = text;
            this.onClickListener = onClickListener;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView icon;

        ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_userpaymentmethod_list_entry, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Entry e = entries.get(position);

            holder.icon.setImageResource(e.drawableRes);
            holder.text.setText(e.text);
            holder.itemView.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}
