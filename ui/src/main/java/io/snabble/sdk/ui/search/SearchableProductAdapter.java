package io.snabble.sdk.ui.search;


import android.database.Cursor;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.OperationCanceledException;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.StringNormalizer;

public class SearchableProductAdapter extends RecyclerView.Adapter {
    public enum SearchType {
        BARCODE,
        FOLDED_NAME
    }

    private static Handler backgroundHandler;
    private Handler uiHandler;

    private OnProductSelectedListener productSelectedListener;
    private SearchType searchType = SearchType.FOLDED_NAME;
    private ProductDatabase productDatabase;
    private Cursor cursor;
    private CancellationSignal cancellationSignal;
    private int itemCount = 0;
    private boolean showBarcode = true;
    private String lastQuery = "";

    public SearchableProductAdapter() {
        this(SnabbleUI.getProject().getProductDatabase());
    }

    public SearchableProductAdapter(ProductDatabase productDatabase) {
        this.productDatabase = productDatabase;

        if (backgroundHandler == null) {
            HandlerThread thread = new HandlerThread("SearchableProductAdapter");
            thread.start();
            backgroundHandler = new Handler(thread.getLooper());
        }

        uiHandler = new Handler(Looper.getMainLooper());
    }

    private class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView code;

        ProductViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.name);
            code = view.findViewById(R.id.code);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ProductViewHolder(inflater.inflate(R.layout.item_searchable_product, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        ProductViewHolder holder = (ProductViewHolder) viewHolder;

        cursor.moveToPosition(position);
        final Product product = productDatabase.productAtCursor(cursor);

        holder.name.setText(product.getName());

        String[] scannableCodes = product.getScannableCodes();

        String selectedCode = null;
        if (scannableCodes.length > 0) {
            selectedCode = product.getScannableCodes()[0];
        }

        if (showBarcode) {
            for (String code : product.getScannableCodes()) {
                if (code.contains(lastQuery)) {
                    if (code.startsWith("00000")) {
                        code = code.replace("00000", "");
                    }

                    Spannable spannable = highlight(lastQuery, code);
                    holder.code.setText(spannable);
                    selectedCode = code;
                    break;
                }
            }
        } else {
            holder.code.setVisibility(View.GONE);
        }

        if (productSelectedListener != null) {
            final String finalCode = selectedCode;

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    productSelectedListener.onProductSelected(finalCode);
                }
            });
        }
    }

    public void setShowBarcode(boolean showBarcode) {
        this.showBarcode = showBarcode;
    }

    public void setSearchType(SearchType type) {
        searchType = type;
    }

    public void setOnProductSelectedListener(OnProductSelectedListener productSelectedListener) {
        this.productSelectedListener = productSelectedListener;
    }

    public void search(final String searchQuery) {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }

        cancellationSignal = new CancellationSignal();

        if (searchQuery != null && searchQuery.length() > 0) {
            final String normalizedSearchQuery = StringNormalizer.normalize(searchQuery);
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Cursor newCursor;

                        if (searchType == SearchType.BARCODE) {
                            newCursor = productDatabase.searchByCode(normalizedSearchQuery,
                                    cancellationSignal);
                        } else {
                            newCursor = productDatabase.searchByFoldedName(normalizedSearchQuery,
                                    cancellationSignal);
                        }

                        if (newCursor != null) {
                            final int count = newCursor.getCount();

                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (cursor != null && !cursor.isClosed()) {
                                        cursor.close();
                                    }
                                    cursor = newCursor;

                                    itemCount = count;
                                    lastQuery = searchQuery;
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    } catch (OperationCanceledException e) {

                    }
                }
            });
        } else {
            itemCount = 0;
            notifyDataSetChanged();
        }
    }

    private SpannableString highlight(final String query, final String text) {
        final String normalizedText = StringNormalizer.normalize(text);

        final SpannableString sb = new SpannableString(text);
        final String[] queries = query.split(" ");

        for (String q : queries) {
            if (q.length() > 0) {
                int lastIndex = 0;
                while (true) {
                    lastIndex = normalizedText.indexOf(q, lastIndex);

                    if (lastIndex == -1) {
                        break;
                    }
                    final StyleSpan styleSpan = new StyleSpan(android.graphics.Typeface.BOLD);
                    sb.setSpan(styleSpan, lastIndex, lastIndex + q.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    lastIndex += q.length();
                }
            }
        }

        return sb;
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }
}