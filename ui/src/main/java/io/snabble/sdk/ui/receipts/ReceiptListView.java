package io.snabble.sdk.ui.receipts;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.snabble.sdk.ReceiptInfo;
import io.snabble.sdk.Receipts;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ReceiptListView extends FrameLayout implements Receipts.OnReceiptsUpdateListener {
    private List<ReceiptInfo> receiptList;
    private RecyclerView recyclerView;
    private View emptyState;

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

        emptyState = v.findViewById(R.id.empty_state);

        update();
    }

    private void update() {
        List<ReceiptInfo> newReceiptList = new ArrayList<>();
        ReceiptInfo[] receipts = Snabble.getInstance().getReceipts().getReceiptInfos();

        for (ReceiptInfo receiptInfo : receipts) {
            if (receiptInfo.isDownloaded() || receiptInfo.getProject() != null) {
                newReceiptList.add(receiptInfo);
            }
        }

        receiptList = newReceiptList;
        recyclerView.getAdapter().notifyDataSetChanged();

        if (receiptList.size() == 0) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onReceiptsUpdated() {
        update();
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

            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            date.setText(dateFormat.format(new Date(receiptInfo.getTimestamp())));

            rootView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (receiptInfo.isDownloaded()) {
                        openPdf();
                    } else {
                        final Receipts receipts = Snabble.getInstance().getReceipts();

                        final ProgressDialog progressDialog = new ProgressDialog(getContext());
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setCancelable(false);
                        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                                    receipts.cancelDownload(receiptInfo);
                                    return true;
                                }
                                return false;
                            }
                        });

                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        receipts.download(receiptInfo, new Receipts.ReceiptDownloadCallback() {
                            @Override
                            public void success(ReceiptInfo receiptInfo) {
                                openPdf();
                                progressDialog.cancel();
                            }

                            @Override
                            public void failure() {
                                progressDialog.cancel();

                                UIUtils.snackbar(ReceiptListView.this,
                                        R.string.Snabble_Receipt_errorDownload,
                                        UIUtils.SNACKBAR_LENGTH_VERY_LONG).show();
                            }
                        });
                    }
                }

                private void openPdf() {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Uri uri = FileProvider.getUriForFile(getContext(),
                            getContext().getPackageName() + ".ReceiptFileProvider",
                            new File(receiptInfo.getFilePath()));

                    intent.setDataAndType(uri, "application/pdf");

                    Activity activity = UIUtils.getHostActivity(getContext());
                    PackageManager pm = activity.getPackageManager();
                    if (intent.resolveActivity(pm) != null) {
                        activity.startActivity(intent);
                    } else {
                        UIUtils.snackbar(ReceiptListView.this,
                                R.string.Snabble_Receipt_pdfReaderUnavailable,
                                UIUtils.SNACKBAR_LENGTH_VERY_LONG).show();
                    }
                }
            });
        }
    }

    public void registerListeners() {
        Snabble.getInstance().getReceipts().addOnUpdateListener(this);
    }

    public void unregisterListeners() {
        Snabble.getInstance().getReceipts().removeOnUpdateListener(this);
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

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return new ViewHolder(inflater.inflate(R.layout.item_receipt, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindTo(receiptList.get(position));
        }

        @Override
        public int getItemCount() {
            return receiptList != null ? receiptList.size() : 0;
        }
    }
}
