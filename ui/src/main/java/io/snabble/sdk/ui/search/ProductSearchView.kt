package io.snabble.sdk.ui.search

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager
import io.snabble.sdk.ui.payment.creditcard.shared.widget.TextInput
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.isNotNullOrBlank

open class ProductSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val searchableProductAdapter: SearchableProductAdapter
    private val composeContainer: ComposeView
    private val addCodeAsIs: TextView
    private var lastSearchQuery: String? = null

    var searchBarEnabled = true
        set(value) {
            field = value
            if (value) {
                composeContainer.visibility = VISIBLE
            } else {
                composeContainer.visibility = GONE
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(composeContainer.windowToken, 0)
                composeContainer.clearFocus()
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

    init {
        inflate(context, R.layout.snabble_view_search_product, this)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)

        composeContainer = findViewById(R.id.compose_container)
        composeContainer.setContent {
            ThemeWrapper {
                "Using wrapper".xx()
                val textFieldManager = rememberTextFieldManager()
                val focusRequester = remember { FocusRequester() }

                var searchedCode by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    textFieldManager.showKeyboard()
                }
                TextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = searchedCode,
                    label = stringResource(id = R.string.Snabble_Scanner_enterBarcode),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            textFieldManager.clearFocusAndHideKeyboard()
                            showScannerWithCode(searchedCode)
                        }
                    ),
                    onValueChanged = { value ->
                        if (searchBarEnabled) {
                            searchedCode = value
                            search(value)
                        }
                    }
                )

            }
        }
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
            SearchHelper.lastSearch = scannableCode
            SnabbleUI.executeAction(context, SnabbleUI.Event.GO_BACK)
        }
    }

    fun search(searchQuery: String) {
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

    /** allows for overriding the default action (calling SnabbleUICallback)  */
    fun setOnProductSelectedListener(listener: OnProductSelectedListener) {
        productSelectedListener = listener
    }
}
