package io.snabble.sdk.ui.payment;

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
import io.snabble.sdk.ui.R;

public class UserPaymentMethodListView extends FrameLayout {
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

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new UserPaymentMethodListView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        layoutManager.scrollToPosition(0);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        int dividerColor = ResourcesCompat.getColor(getResources(), R.color.snabble_dividerColor, null);
        dividerItemDecoration.setDrawable(new ColorDrawable(dividerColor));
        recyclerView.addItemDecoration(dividerItemDecoration);
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

    private class Adapter extends RecyclerView.Adapter<UserPaymentMethodListView.ViewHolder> {
        @Override
        public UserPaymentMethodListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_userpaymentmethod_list, parent, false);
            return new UserPaymentMethodListView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final UserPaymentMethodListView.ViewHolder holder, final int position) {

        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

}
