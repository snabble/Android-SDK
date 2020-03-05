package io.snabble.sdk.ui.search;


import android.database.Cursor;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.StringNormalizer;

public class SearchableProductAdapter extends RecyclerView.Adapter {
    public enum SearchType {
        BARCODE,
        FOLDED_NAME
    }

    private OnProductSelectedListener productSelectedListener;
    private SearchType searchType = SearchType.FOLDED_NAME;
    private ProductDatabase productDatabase;
    private Cursor cursor;
    private CancellationSignal cancellationSignal;
    private int itemCount = 0;
    private boolean showBarcode = true;
    private String lastQuery = "";
    private Project project;
    private boolean showSku;

    public SearchableProductAdapter() {
        this.project = SnabbleUI.getProject();
        this.productDatabase = project.getProductDatabase();
    }

    @Deprecated
    public SearchableProductAdapter(ProductDatabase unused) {
        this();
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
        return new ProductViewHolder(inflater.inflate(R.layout.snabble_item_searchable_product, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        ProductViewHolder holder = (ProductViewHolder) viewHolder;

        cursor.moveToPosition(position);
        final Product product = productDatabase.productAtCursor(cursor);

        holder.name.setText(product.getName());

        Product.Code[] scannableCodes = product.getScannableCodes();

        List<String> searchableTemplates = Arrays.asList(project.getSearchableTemplates());
        Product.Code selectedCode = null;

        for (Product.Code code : scannableCodes) {
            if (searchableTemplates.contains(code.template)) {
                selectedCode = code;
            }
        }

        if (selectedCode == null && scannableCodes.length > 0) {
            selectedCode = scannableCodes[0];
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        if (showBarcode) {
            for (Product.Code code : product.getScannableCodes()) {
                String lookupCode = code.lookupCode;
                if (lookupCode.contains(lastQuery) && searchableTemplates.contains(code.template)) {
                    if (lookupCode.startsWith("00000")) {
                        lookupCode = lookupCode.replace("00000", "");
                    }

                    Spannable spannable = highlight(lastQuery, lookupCode);
                    ssb.append(spannable);
                    selectedCode = code;
                    break;
                }
            }
        }

        if (showSku) {
            if (ssb.length() > 0) {
                ssb.append(" (");
                ssb.append(product.getSku());
                ssb.append(")");
            } else {
                ssb.append(product.getSku());
            }
        }

        if (ssb.length() > 0) {
            holder.code.setText(ssb);
        } else {
            holder.code.setVisibility(View.GONE);
        }

        if (productSelectedListener != null && selectedCode != null) {
            final Product.Code finalCode = selectedCode;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    productSelectedListener.onProductSelected(finalCode.lookupCode);
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

    public void setShowSku(boolean showSku) {
        this.showSku = showSku;
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

            Dispatch.background(() -> {
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

                        Dispatch.mainThread(() -> {
                            if (cursor != null && !cursor.isClosed()) {
                                cursor.close();
                            }
                            cursor = newCursor;

                            itemCount = count;
                            lastQuery = searchQuery;
                            notifyDataSetChanged();
                        });
                    }
                } catch (OperationCanceledException e) {

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
                    sb.setSpan(styleSpan, lastIndex, lastIndex + q.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
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