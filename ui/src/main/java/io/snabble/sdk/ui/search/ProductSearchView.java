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
                search(s.toString());
            }
        });

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    SnabbleUICallback callback = SnabbleUI.getUiCallback();
                    if (callback != null) {
                        CharSequence text = searchBar.getText();
                        if (text != null) {
                            callback.showScannerWithCode(text.toString());
                        }
                    }

                    return true;
                }

                return false;
            }
        });

        addCodeAsIs = findViewById(R.id.add_code_as_is);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchableProductAdapter = new SearchableProductAdapter(project.getProductDatabase());
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
        Telemetry.event(Telemetry.Event.ManuallyEnteredProduct, scannableCode);

        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.showScannerWithCode(scannableCode);
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

    public void search(String searchQuery) {
        if (searchBarEnabled) {
            searchBar.setText(searchQuery);
        }

        lastSearchQuery = searchQuery;
        searchableProductAdapter.search(searchQuery);
    }

    private void onSearchUpdated() {
        if (searchableProductAdapter.getItemCount() == 0
                && lastSearchQuery != null && lastSearchQuery.length() > 0) {
            addCodeAsIs.setText(getResources().getString(R.string.Snabble_Scanner_addCodeAsIs, lastSearchQuery));
            addCodeAsIs.setVisibility(View.VISIBLE);
        } else {
            addCodeAsIs.setVisibility(View.GONE);
        }
    }
}
