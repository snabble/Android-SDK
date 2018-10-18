package io.snabble.sdk.ui.receipts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.snabble.sdk.ReceiptInfo;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.UIUtils;

public class ReceiptListView extends FrameLayout {
    private ReceiptInfo[] receiptList;
    private RecyclerView recyclerView;

    public ReceiptListView(@NonNull Context context) {
        super(context);

        init();
    }

    public ReceiptListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public ReceiptListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        View v = View.inflate(getContext(), R.layout.view_receipt_list, this);

        recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new Adapter());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        update();
    }

    private void update() {
        receiptList = Snabble.getInstance().getReceipts().getReceiptInfos();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView shopName;
        TextView price;
        TextView date;
        View rootView;

        public ViewHolder(View itemView) {
            super(itemView);

            shopName = itemView.findViewById(R.id.shop_name);
            price = itemView.findViewById(R.id.price);
            date = itemView.findViewById(R.id.date);

            rootView = itemView;
        }

        public void bindTo(final ReceiptInfo receiptInfo){
            shopName.setText(receiptInfo.getShopName());
            price.setText(receiptInfo.getPrice());

            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            date.setText(dateFormat.format(new Date(receiptInfo.getTimestamp())));

            rootView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Uri uri = FileProvider.getUriForFile(getContext(),
                            getContext().getPackageName(),
                            new File(receiptInfo.getFilePath()));

                    intent.setDataAndType(uri, "application/pdf");

                    Activity activity = UIUtils.getHostActivity(getContext());
                    PackageManager pm = activity.getPackageManager();
                    if (intent.resolveActivity(pm) != null) {
                        activity.startActivity(intent);
                    }
                }
            });
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return new ViewHolder(inflater.inflate(R.layout.item_receipt, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindTo(receiptList[position]);
        }

        @Override
        public int getItemCount() {
            return receiptList != null ? receiptList.length : 0;
        }
    }
}
