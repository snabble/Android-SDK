package io.snabble.sdk.ui.search

import android.database.Cursor
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.*
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.StringNormalizer

class SearchableProductAdapter : RecyclerView.Adapter<SearchableProductAdapter.ProductViewHolder>() {
    enum class SearchType(val isFreeText: Boolean, val hasQuantity: Boolean) {
        BARCODE(false, false),
        FOLDED_NAME(false, false),
        FREE_TEXT(true, false),
        FREE_TEXT_WITH_QUANTITY(true, true)
    }

    var productSelectedListener: OnProductSelectedListener? = null
    var searchType = SearchType.FOLDED_NAME // TODO might add change listener to update the ui...
    private var cursor: Cursor? = null
    private var cancellationSignal: CancellationSignal? = null
    private var itemCount = 0
    var showBarcode = true
    private var searchQuery = ""
    lateinit var project: Project
    private val productDatabase by lazy { project.productDatabase }
    var showSku = false
    var quantityManager: QuantityManager? = null

    interface QuantityManager {
        fun getQuantityFor(product: Product): Int
        fun onQuantityChanged(product: Product, quantity: Int)
        fun onProductSelected(product: Product)
    }

    abstract class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bindTo(product: Product, highlight: String, quantityManager: QuantityManager?)
    }

    private inner class BasicProductViewHolder(inflater: LayoutInflater, parent: ViewGroup) : ProductViewHolder(inflater.inflate(R.layout.snabble_item_searchable_product, parent, false)) {
        var name: TextView = itemView.findViewById(R.id.name)
        var code: TextView = itemView.findViewById(R.id.code)

        override fun bindTo(product: Product, highlight: String, quantityManager: QuantityManager?) {
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

    private class ExtendedProductViewHolder(inflater: LayoutInflater, parent: ViewGroup) : ProductViewHolder(inflater.inflate(R.layout.snabble_item_product_with_quantity, parent, false)) {
        private val title: TextView = itemView.findViewById(R.id.name)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val count: TextView = itemView.findViewById(R.id.count)
        private val image: ImageView = itemView.findViewById(R.id.image)
        private val plus: ImageView = itemView.findViewById(R.id.plus)
        private val minus: ImageView = itemView.findViewById(R.id.minus)
        private var quantity: Int = 0

        private val Number.dpInPx: Int
            get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), title.context.resources.displayMetrics).toInt()

        init {
            val padding = 3.dpInPx
            count.setPadding(padding, padding, padding, padding)
            count.minWidth = 36.dpInPx
            count.gravity = Gravity.CENTER
        }

        override fun bindTo(product: Product, highlight: String, quantityManager: QuantityManager?) {
            itemView.setOnClickListener {
                quantityManager?.onProductSelected(product)
            }
            plus.setOnClickListener {
                quantity++
                quantityManager?.onQuantityChanged(product, quantity)
                updateQuantity()
            }
            minus.setOnClickListener {
                quantity = Integer.max(0, quantity - 1)
                quantityManager?.onQuantityChanged(product, quantity)
                updateQuantity()
            }
            minus.setOnLongClickListener {
                quantity = 0
                quantityManager?.onQuantityChanged(product, quantity)
                updateQuantity()
                true
            }

            title.text = product.name.highlight(highlight)

            image.setImageDrawable(null)
            image.isVisible = true//item.shouldShowImage
            subtitle.setOrHide(product.description)
            if (product.imageUrl.isNotNullOrBlank()) {
                Picasso.get()
                        .load(product.imageUrl)
                        .into(image)
            }
            if(quantityManager != null) {
                quantity = quantityManager.getQuantityFor(product)
                updateQuantity()
            } else {
                minus.isVisible = false
                plus.isVisible = false
                count.isVisible = false
            }
        }

        private fun updateQuantity() {
            count.text = quantity.toString()
            count.isVisible = true
            minus.isVisible = quantity > 0
            plus.isVisible = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // TODO move layout from code to sdk
        return if(searchType.isFreeText) {
            if(searchType.hasQuantity) {
                requireNotNull(quantityManager, { "Cannot create a view holder with quantity support without a quantityManager" })
            }
            ExtendedProductViewHolder(inflater, parent)
        } else {
            BasicProductViewHolder(inflater, parent)
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        if(position == 0 && searchType.isFreeText) {
            holder.bindTo(Product.Builder().setName(searchQuery).build(), "", quantityManager)
        } else {
            cursor!!.moveToPosition(position - offset)
            val product = productDatabase.productAtCursor(cursor)
            holder.bindTo(product, searchQuery, quantityManager)
        }
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

    private val offset
        get() = if(searchType.isFreeText && searchQuery.isNotBlank()) 1 else 0

    override fun getItemCount() = itemCount + offset

    fun setOnProductSelectedListener(productSelectedListener: OnProductSelectedListener?) {
        this.productSelectedListener = productSelectedListener
    }
}