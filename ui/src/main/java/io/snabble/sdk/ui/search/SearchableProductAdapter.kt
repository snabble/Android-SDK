package io.snabble.sdk.ui.search

import android.database.Cursor
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Product
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.highlight
import io.snabble.sdk.ui.utils.setOrHide
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.StringNormalizer

class SearchableProductAdapter : RecyclerView.Adapter<SearchableProductAdapter.ProductViewHolder>() {
    enum class SearchType {
        BARCODE, FOLDED_NAME
    }

    private var productSelectedListener: OnProductSelectedListener? = null
    private var searchType = SearchType.FOLDED_NAME
    private var cursor: Cursor? = null
    private var cancellationSignal: CancellationSignal? = null
    private var itemCount = 0
    var showBarcode = true
    private var searchQuery = ""
    private val project = SnabbleUI.getProject()
    private val productDatabase by lazy { project.productDatabase }
    var showSku = false

    inner class ProductViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.snabble_item_searchable_product, parent, false)) {
        var name: TextView = itemView.findViewById(R.id.name)
        var code: TextView = itemView.findViewById(R.id.code)

        fun bindTo(product: Product, highlight: String) {
            name.text = product.name
            val scannableCodes = product.scannableCodes
            val searchableTemplates = project.searchableTemplates.toList()
            var selectedCode: Product.Code? = null
            for (code in scannableCodes) {
                if (searchableTemplates.contains(code.template)) {
                    selectedCode = code
                }
            }
            if (scannableCodes?.isNotEmpty() == true) {
                selectedCode = scannableCodes[0]
            }
            val ssb = SpannableStringBuilder()
            if (showBarcode) {
                for (code in product.scannableCodes) {
                    var lookupCode = code.lookupCode
                    if (lookupCode.contains(highlight) && searchableTemplates.contains(code.template)) {
                        if (lookupCode.startsWith("00000")) {
                            lookupCode = lookupCode.replace("00000", "")
                        }
                        val spannable = lookupCode.highlight(highlight)
                        ssb.append(spannable)
                        selectedCode = code
                        break
                    }
                }
            }
            if (showSku) {
                if (ssb.isNotEmpty()) {
                    ssb.append(" (")
                    ssb.append(product.sku)
                    ssb.append(")")
                } else {
                    ssb.append(product.sku)
                }
            }
            code.setOrHide(ssb)
            if (productSelectedListener != null && selectedCode != null) {
                val finalCode: Product.Code = selectedCode
                itemView.setOnClickListener { productSelectedListener?.onProductSelected(finalCode.lookupCode) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ProductViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        cursor!!.moveToPosition(position)
        val product = productDatabase.productAtCursor(cursor)
        holder.bindTo(product, searchQuery)
    }

    fun search(searchQuery: String?) {
        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()
        if (searchQuery != null && searchQuery.isNotEmpty()) {
            val normalizedSearchQuery = StringNormalizer.normalize(searchQuery)
            Dispatch.background {
                try {
                    val newCursor = if (searchType == SearchType.BARCODE) {
                        productDatabase.searchByCode(normalizedSearchQuery, cancellationSignal)
                    } else {
                        productDatabase.searchByFoldedName(normalizedSearchQuery, cancellationSignal)
                    }
                    if (newCursor != null) {
                        val count = newCursor.count
                        Dispatch.mainThread {
                            if (cursor?.isClosed == false) {
                                cursor?.close()
                            }
                            cursor = newCursor
                            itemCount = count
                            this.searchQuery = searchQuery
                            notifyDataSetChanged()
                        }
                    }
                } catch (e: OperationCanceledException) {
                }
            }
        } else {
            itemCount = 0
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = itemCount

    fun setOnProductSelectedListener(productSelectedListener: OnProductSelectedListener?) {
        this.productSelectedListener = productSelectedListener
    }
}