package io.snabble.sdk.ui.search

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.isNotNullOrBlank

open class ProductSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val searchableProductAdapter: SearchableProductAdapter
    private val searchBar: TextInputEditText
    private val searchBarTextInputLayout: TextInputLayout
    private val addCodeAsIs: TextView
    private var lastSearchQuery: String? = null

    var searchBarEnabled = true
        set(value) {
            field = value
            if (value) {
                searchBarTextInputLayout.visibility = VISIBLE
            } else {
                searchBarTextInputLayout.visibility = GONE
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchBarTextInputLayout.windowToken, 0)
                searchBarTextInputLayout.clearFocus()
            }
        }

    /** if set to true allows adding any code, regardless if an product is found  */
    var allowAnyCode = false

    private var productSelectedListener: OnProductSelectedListener? = null
    var showSku = false
        set(value) {
            field = value
            searchableProductAdapter.showSku = value
        }
    var showBarcode = true
        set(value) {
            field = value
            searchableProductAdapter.showBarcode = value
        }
    var inputType: Int
        get() = searchBar.inputType
        set(value) {
            searchBar.inputType = value
        }

    init {
        inflate(context, R.layout.snabble_view_search_product, this)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)

        searchBarTextInputLayout = findViewById(R.id.search_bar_layout)
        searchBar = findViewById(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (searchBarEnabled) {
                    search(s.toString())
                }
            }
        })
        searchBar.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                searchBar.text?.let { text ->
                    showScannerWithCode(text.toString())
                }
                return@OnEditorActionListener true
            }
            false
        })
        searchBarTextInputLayout.requestFocus()
        addCodeAsIs = findViewById(R.id.add_code_as_is)
        recyclerView.layoutManager = LinearLayoutManager(context)
        searchableProductAdapter = SearchableProductAdapter()
        searchableProductAdapter.showBarcode = true
        searchableProductAdapter.showSku = showSku
        searchableProductAdapter.setOnProductSelectedListener(::showScannerWithCode)
        searchableProductAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                onSearchUpdated()
                requestLayout()
            }
        })
        addCodeAsIs.setOnClickListener { showScannerWithCode(lastSearchQuery) }
        recyclerView.adapter = searchableProductAdapter

        searchBarEnabled = true
    }

    private fun showScannerWithCode(scannableCode: String?) {
        productSelectedListener?.onProductSelected(scannableCode) ?: run {
            Telemetry.event(Telemetry.Event.ManuallyEnteredProduct, scannableCode)
            val callback = SnabbleUI.getUiCallback()
            val args = Bundle()
            args.putString("showProductCode", scannableCode)
            SnabbleUI.executeAction(SnabbleUI.Action.SHOW_SCANNER, args)
        }
    }

    fun search(searchQuery: String) {
        if (searchBarEnabled && searchBar.text.toString() != searchQuery) {
            searchBar.setText(searchQuery)
        }
        if (lastSearchQuery == null || lastSearchQuery != searchQuery) {
            lastSearchQuery = searchQuery
            searchableProductAdapter.search(searchQuery)
        }
    }

    private fun onSearchUpdated() {
        if (searchableProductAdapter.itemCount == 0 && lastSearchQuery.isNotNullOrBlank() && allowAnyCode) {
            addCodeAsIs.text = resources.getString(R.string.Snabble_Scanner_addCodeAsIs, lastSearchQuery)
            addCodeAsIs.visibility = VISIBLE
        } else {
            addCodeAsIs.visibility = GONE
        }
    }

    fun focusTextInputAndShowKeyboard() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            searchBar.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchBar, 0)
        }, 100)
    }

    fun focusTextInput() = searchBarTextInputLayout.requestFocus()

    /** allows for overriding the default action (calling SnabbleUICallback)  */
    fun setOnProductSelectedListener(listener: OnProductSelectedListener) {
        productSelectedListener = listener
    }
}