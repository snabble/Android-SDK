package io.snabble.sdk.ui.search;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.Project;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;

public class ProductSearchView extends FrameLayout {
    private SearchableProductAdapter searchableProductAdapter;
    private EditText searchBar;
    private boolean searchBarEnabled;
    private TextView addCodeAsIs;
    private String lastSearchQuery;
    private boolean allowAnyCode;
    private OnProductSelectedListener onProductSelectedListener;

    public ProductSearchView(Context context) {
        super(context);
        inflateView();
    }

    public ProductSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public ProductSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_search_product, this);

        Project project = SnabbleUI.getProject();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        searchBarEnabled = true;
        allowAnyCode = true;
        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchBarEnabled) {
                    search(s.toString());
                }
            }
        });

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    CharSequence text = searchBar.getText();
                    if (text != null) {
                        showScannerWithCode(text.toString());
                    }

                    return true;
                }

                return false;
            }
        });

        addCodeAsIs = findViewById(R.id.add_code_as_is);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchableProductAdapter = new SearchableProductAdapter();
        searchableProductAdapter.setShowBarcode(true);
        searchableProductAdapter.setSearchType(SearchableProductAdapter.SearchType.BARCODE);
        searchableProductAdapter.setOnProductSelectedListener(new OnProductSelectedListener() {
            @Override
            public void onProductSelected(String scannableCode) {
                showScannerWithCode(scannableCode);
            }
        });

        searchableProductAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                onSearchUpdated();
            }
        });

        addCodeAsIs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showScannerWithCode(lastSearchQuery);
            }
        });

        recyclerView.setAdapter(searchableProductAdapter);
    }

    private void showScannerWithCode(String scannableCode) {
        if (onProductSelectedListener != null) {
            onProductSelectedListener.onProductSelected(scannableCode);
        } else {
            Telemetry.event(Telemetry.Event.ManuallyEnteredProduct, scannableCode);

            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.showScannerWithCode(scannableCode);
            }
        }
    }

    public void search(String searchQuery) {
        if (searchBarEnabled) {
            if(!searchBar.getText().toString().equals(searchQuery)) {
                searchBar.setText(searchQuery);
            }
        }

        if (lastSearchQuery == null || !lastSearchQuery.equals(searchQuery)) {
            lastSearchQuery = searchQuery;
            searchableProductAdapter.search(searchQuery);
        }
    }

    private void onSearchUpdated() {
        if (searchableProductAdapter.getItemCount() == 0
                && lastSearchQuery != null && lastSearchQuery.length() > 0
                && allowAnyCode) {
            addCodeAsIs.setText(getResources().getString(R.string.Snabble_Scanner_addCodeAsIs, lastSearchQuery));
            addCodeAsIs.setVisibility(View.VISIBLE);
        } else {
            addCodeAsIs.setVisibility(View.GONE);
        }
    }

    public void setSearchBarEnabled(boolean searchBarEnabled) {
        this.searchBarEnabled = searchBarEnabled;

        if (searchBarEnabled) {
            searchBar.setVisibility(View.VISIBLE);
        } else {
            searchBar.setVisibility(View.GONE);
        }
    }

    /** if set to true allows adding any code, regardless if an product is found **/
    public void setAllowAnyCode(boolean allowAnyCode) {
        this.allowAnyCode = allowAnyCode;
    }

    /** allows for overriding the default action (calling SnabbleUICallback) **/
    public void setOnProductSelectedListener(OnProductSelectedListener onProductSelectedListener) {
        this.onProductSelectedListener = onProductSelectedListener;
    }
}
