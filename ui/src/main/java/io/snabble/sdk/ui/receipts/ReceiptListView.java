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
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.ReceiptInfo;
import io.snabble.sdk.Receipts;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ReceiptListView extends CoordinatorLayout implements Checkout.OnCheckoutStateChangedListener {
    private ReceiptInfo[] receiptInfos;
    private RecyclerView recyclerView;
    private View emptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Receipts receipts;
    private Checkout checkout;
    private boolean showProgressBar;

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
        View v = View.inflate(getContext(), R.layout.snabble_view_receipt_list, this);

        receipts = Snabble.getInstance().getReceipts();

        try {
            checkout = SnabbleUI.getProject().getCheckout();
        } catch (RuntimeException ignored) {
            // TODO better way to handle missing project
        }

        recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new Adapter());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        swipeRefreshLayout = v.findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update(true);
            }
        });

        emptyState = v.findViewById(R.id.empty_state);

        update(true);
    }

    private void update(boolean showRefresh) {
        swipeRefreshLayout.setRefreshing(showRefresh);

        receipts.getReceiptInfos(new Receipts.ReceiptInfoCallback() {
            @Override
            public void success(ReceiptInfo[] newReceiptInfos) {
                swipeRefreshLayout.setRefreshing(false);

                receiptInfos = newReceiptInfos;

                if (checkout != null && checkout.getState() == Checkout.State.PAYMENT_APPROVED) {
                    boolean containsOrder = false;
                    for (ReceiptInfo receiptInfo : newReceiptInfos) {
                        if (receiptInfo.getId().equals(checkout.getOrderId())) {
                            containsOrder = true;
                            break;
                        }
                    }

                    if (!containsOrder) {
                        showProgressBar = true;
                    }
                }

                recyclerView.getAdapter().notifyDataSetChanged();

                updateEmptyState();
            }

            @Override
            public void failure() {
                swipeRefreshLayout.setRefreshing(false);

                receiptInfos = null;
                recyclerView.getAdapter().notifyDataSetChanged();

                updateEmptyState();
                UIUtils.snackbar(ReceiptListView.this, R.string.Snabble_networkError, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show();
            }
        });
    }

    private void updateEmptyState() {
        if ((receiptInfos == null || receiptInfos.length == 0 && !showProgressBar)) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void registerListeners() {
        if (checkout != null) {
            checkout.addOnCheckoutStateChangedListener(this);
        }
    }

    private void unregisterListeners() {
        if (checkout != null) {
            checkout.removeOnCheckoutStateChangedListener(this);
        }
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

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.RECEIPT_AVAILABLE) {
            showProgressBar = false;
            update(false);
        }
    }

    private class GeneratingViewHolder extends RecyclerView.ViewHolder {
        TextView shopName;

        public GeneratingViewHolder(@NonNull View itemView) {
            super(itemView);

            shopName = itemView.findViewById(R.id.shop_name);
            shopName.setText(checkout.getShop().getName());
        }
    }

    private class ReceiptViewHolder extends RecyclerView.ViewHolder {
        TextView shopName;
        TextView price;
        TextView date;
        View rootView;

        public ReceiptViewHolder(View itemView) {
            super(itemView);

            shopName = itemView.findViewById(R.id.shop_name);
            price = itemView.findViewById(R.id.price);
            date = itemView.findViewById(R.id.date);

            rootView = itemView;
        }

        public void bindTo(final ReceiptInfo receiptInfo){
            shopName.setText(receiptInfo.getShopName());
            price.setText(receiptInfo.getPrice());

            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
            date.setText(dateFormat.format(receiptInfo.getDate()));

            rootView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
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
                                receipts.cancelDownload();
                                return true;
                            }
                            return false;
                        }
                    });

                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    receipts.download(receiptInfo, new Receipts.ReceiptDownloadCallback() {
                        @Override
                        public void success(File pdf) {
                            progressDialog.cancel();

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            Uri uri = FileProvider.getUriForFile(getContext(),
                                    getContext().getPackageName() + ".ReceiptFileProvider", pdf);
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

                        @Override
                        public void failure() {
                            progressDialog.cancel();
                            UIUtils.snackbar(ReceiptListView.this,
                                    R.string.Snabble_Receipt_errorDownload,
                                    UIUtils.SNACKBAR_LENGTH_VERY_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_PROGRESS_BAR = 0;
        private static final int TYPE_RECEIPT = 1;
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());

            if (viewType == TYPE_PROGRESS_BAR) {
                return new GeneratingViewHolder(inflater.inflate(R.layout.snabble_item_receipt_generating, parent, false));
            }

            return new ReceiptViewHolder(inflater.inflate(R.layout.snabble_item_receipt, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            if (showProgressBar && position == 0) {
                return TYPE_PROGRESS_BAR;
            }

            return TYPE_RECEIPT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_RECEIPT) {
                int pos = showProgressBar ? position - 1 : position;
                ((ReceiptViewHolder) holder).bindTo(receiptInfos[pos]);
            }
        }

        @Override
        public int getItemCount() {
            return (showProgressBar ? 1 : 0) + (receiptInfos != null ? receiptInfos.length : 0);
        }
    }
}
