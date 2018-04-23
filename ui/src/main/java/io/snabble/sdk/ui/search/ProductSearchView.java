package io.snabble.sdk.ui.search;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;

public class ProductSearchView extends FrameLayout {
    private RecyclerView recyclerView;
    private SearchableProductAdapter searchableProductAdapter;
    private EditText searchBar;
    private boolean searchBarEnabled;

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

        recyclerView = findViewById(R.id.recycler_view);
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
                searchableProductAdapter.search(s.toString());
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

        setSdkInstance(SnabbleUI.getSdkInstance());
    }

    public void setSdkInstance(SnabbleSdk sdkInstance) {
        if (sdkInstance == null) {
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchableProductAdapter = new SearchableProductAdapter(sdkInstance.getProductDatabase());
        searchableProductAdapter.setShowBarcode(true);
        searchableProductAdapter.setSearchType(SearchableProductAdapter.SearchType.BARCODE);
        searchableProductAdapter.setOnProductSelectedListener(new OnProductSelectedListener() {
            @Override
            public void onProductSelected(String scannableCode) {
                Telemetry.event(Telemetry.Event.ManuallyEnteredProduct, scannableCode);

                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showScannerWithCode(scannableCode);
                }
            }
        });
        recyclerView.setAdapter(searchableProductAdapter);
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

        searchableProductAdapter.search(searchQuery);
    }


    public void show() {
        setVisibility(View.VISIBLE);
        searchBar.setText("");

        if (searchBar.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void hide() {
        setVisibility(View.GONE);
    }
}
