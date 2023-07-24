package io.snabble.sdk

import android.util.Log
import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.checkout.PaymentMethodInfo
import io.snabble.sdk.checkout.Violation
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.events.data.EventType
import io.snabble.sdk.events.data.payload.Payload
import io.snabble.sdk.shoppingcart.BackendCartItem
import io.snabble.sdk.shoppingcart.ItemType
import io.snabble.sdk.shoppingcart.ShoppingCartListener
import io.snabble.sdk.shoppingcart.Taxation
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Class representing the snabble shopping cart
 */
class ShoppingCart internal constructor(
    @Transient private val project: Project? = null,
    @Transient private val priceFormatter: PriceFormatter? = null
) : Iterable<ShoppingCart.Item?> {

    private val mutableShoppingCartData: MutableStateFlow<ShoppingCartData> = MutableStateFlow(ShoppingCartData())
    val shoppingCartData: StateFlow<ShoppingCartData> = mutableShoppingCartData.asStateFlow()

    private val mutableOldShoppingCartData: MutableStateFlow<ShoppingCartData?> = MutableStateFlow(ShoppingCartData())
    private val oldShoppingCartData: StateFlow<ShoppingCartData?> = mutableOldShoppingCartData.asStateFlow()

    @Transient
    private val listeners: CopyOnWriteArrayList<ShoppingCartListener> = CopyOnWriteArrayList()

    @Transient
    private var updater: ShoppingCartUpdater? = null

    init {
        updateTimestamp()
        project?.let {
            updater = ShoppingCartUpdater(it, this)
        }
    }

    fun initWithData(data: ShoppingCartData) {
        updateData(data)
        checkForTimeout()
        updatePrices(false)
        notifyCartDataChanged(this)
    }

    private fun updateData(data: ShoppingCartData) {
        mutableShoppingCartData.tryEmit(data)
        mutableOldShoppingCartData.tryEmit(null)
        data.applyShoppingCart(this)
        oldShoppingCartData.value?.applyShoppingCart(this)
    }

    /**
     * The id used to identify this cart session
     */
    val id: String
        get() = shoppingCartData.value.id

    /**
     * Create a new cart item using a product and a scanned code
     */
    fun newItem(product: Product, scannedCode: ScannedCode): Item = Item(this, product, scannedCode)

    /**
     * Create a new cart item using a coupon and a scanned code
     */
    fun newItem(coupon: Coupon, scannedCode: ScannedCode?): Item = Item(this, coupon, scannedCode)

    /**
     * Create a new cart item using a line item of a checkout info
     */
    fun newItem(lineItem: LineItem): Item = Item(this, lineItem)

    /**
     * Add a item to the cart
     */
    fun add(item: Item) = insert(item, 0)

    /**
     * Adds coupons without adding a scanned code to it, you can use this function to quickly
     * add DIGITAL coupons that do not have a barcode associated with them
     */
    fun addCoupon(coupon: Coupon) = add(newItem(coupon, null))

    /**
     * Adds coupons with a scanned code to it, you can use this function to quickly
     * add PRINTED coupons
     */
    fun addCoupon(coupon: Coupon, scannedCode: ScannedCode?) = add(newItem(coupon, scannedCode))

    /**
     * Insert a cart item into the shopping cart at a specific index
     */
    fun insert(item: Item, index: Int) = insert(item, index, true)

    /**
     * Insert a cart item into the shopping cart and updates the prices
     */
    fun insert(item: Item, index: Int, update: Boolean) {
        val itemIsMerged = insertIfMergeable(item, index, update)
        if (itemIsMerged) return
        shoppingCartData.value.items.add(index, item)
        clearBackup()
        checkLimits()
        notifyItemAdded(this, item)

        // sort coupons to bottom
        shoppingCartData.value.items.sortWith(Comparator { item1: Item, item2: Item ->
            val t1 = item1.type
            val t2 = item2.type
            if (t2 == ItemType.COUPON && t1 == ItemType.PRODUCT) {
                return@Comparator -1
            } else if (t1 == ItemType.COUPON && t2 == ItemType.PRODUCT) {
                return@Comparator 1
            } else {
                return@Comparator 0
            }
        })

        if (update) {
            shoppingCartData.value.addCount++
            shoppingCartData.value.modCount++
            generateNewUUID()
            invalidateOnlinePrices()
            updatePrices(true)
        }
    }

    private fun insertIfMergeable(item: Item, index: Int, update: Boolean): Boolean {
        if (item.isMergeable) {
            val existingItem = getExistingMergeableProduct(item.product) ?: return false
            shoppingCartData.value.items.remove(existingItem)
            shoppingCartData.value.items.add(index, item)
            shoppingCartData.value.modCount++
            generateNewUUID()
            checkLimits()
            notifyQuantityChanged(this, item)
            if (update) {
                invalidateOnlinePrices()
                updatePrices(true)
            }
            return true
        }
        return false
    }

    /**
     * Gets the cart item a specific index
     */
    operator fun get(index: Int): Item = shoppingCartData.value.items[index]

    override fun iterator(): MutableIterator<Item> = shoppingCartData.value.items.iterator()

    /**
     * Returns a cart item that contains the given product, if that cart item
     * can be merged.
     *
     *
     * A cart item is not mergeable if it uses encoded shoppingCartData.value of a scanned code (e.g. a different price)
     */
    fun getExistingMergeableProduct(product: Product?): Item? {
        product ?: return null
        shoppingCartData.value.items.forEach { item ->
            if (product == item.product && item.isMergeable) { //Todo: geht das?
                return item
            }
        }
        return null
    }

    /**
     * Find a cart item by it's id
     */
    fun getByItemId(itemId: String?): Item? {
        itemId ?: return null

        shoppingCartData.value.items.forEach { item ->
            if (itemId == item.id) {
                return item
            }
        }
        return null
    }

    /**
     * Gets the current index of a cart item
     */
    fun indexOf(item: Item?): Int = shoppingCartData.value.items.indexOf(item)

    /**
     * Removed a cart item from the cart by its index
     */
    fun remove(index: Int) {
        shoppingCartData.value.modCount++
        generateNewUUID()
        val removedItem = shoppingCartData.value.items.removeAt(index)
        checkLimits()
        updatePrices(size() != 0)
        invalidateOnlinePrices()
        notifyItemRemoved(this, removedItem, index)
    }

    /**
     * The number items in the cart.
     *
     *
     * This is not the sum of articles.
     */
    fun size(): Int = shoppingCartData.value.items.size

    /**
     * Check if the cart is empty
     */
    /**
     * Check if the cart is empty
     */
    val isEmpty: Boolean = shoppingCartData.value.items.isEmpty()

    /**
     * Backups the cart, so it can be restured using [.restore] later.
     *
     *
     * A cart is restorable for up to 5 minutes.
     */
    fun backup() {
        if (shoppingCartData.value.items.size > 0) {
            mutableOldShoppingCartData.tryEmit(shoppingCartData.value.deepCopy())
            shoppingCartData.value.backupTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Check if the cart is backed up by [.backup] and still in the 5 minute time window
     */
    val isRestorable: Boolean
        get() = oldShoppingCartData.value != null && shoppingCartData.value.backupTimestamp > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(
            5
        )

    /**
     * Clears the backup storage of the cart
     */
    fun clearBackup() {
        mutableOldShoppingCartData.tryEmit(null)
        shoppingCartData.value.backupTimestamp = 0
    }

    /**
     * Restores the cart previously backed up by [.backup]
     */
    fun restore() {
        if (isRestorable) {
            oldShoppingCartData.value?.let {
                mutableShoppingCartData.tryEmit(it)
                shoppingCartData.value.applyShoppingCart(this)
                clearBackup()
                checkLimits()
                updatePrices(false)
                notifyProductsUpdate(this)
            }
        }
    }

    /**
     * The last time the cart was backed up by using [.backup]
     *
     * @return
     */
    /**
     * The last time the cart was backed up by using [.backup]
     *
     * @return
     */
    val backupTimestamp: Long = shoppingCartData.value.backupTimestamp

    /**
     * Clears the cart of all items
     */
    fun clear() {
        shoppingCartData.value.items = mutableListOf()
        shoppingCartData.value.modCount = 0
        shoppingCartData.value.addCount = 0
        generateNewUUID()
        shoppingCartData.value.onlineTotalPrice = null
        checkLimits()
        updatePrices(false)
        notifyCleared(this)
    }

    /**
     * Sets the current taxation type of the cart
     */
    /**
     * Gets the current [Taxation] type of the shopping cart
     */
    var taxation: Taxation
        get() = shoppingCartData.value.taxation
        set(taxation) {
            shoppingCartData.value.taxation = taxation
            notifyTaxationChanged(this, taxation)
        }

    /**
     * Clears the cart and generated a cart new session.
     */
    fun invalidate() {
        shoppingCartData.value.id = UUID.randomUUID().toString()
        generateNewUUID()
        clear()
    }

    /**
     * Updates each items products in the shopping cart
     */
    fun updateProducts() {
        val productDatabase = project?.productDatabase
        if (productDatabase?.isUpToDate == true) {
            shoppingCartData.value.items.forEach { item ->
                val product = productDatabase.findByCode(item.scannedCode)
                if (product != null) {
                    item.product = product
                }
            }
            notifyProductsUpdate(this) // Todo: Do we really want this here?
        }
        updatePrices(false) // Todo: Do we really want this here?
    }

    /**
     * Resets the cart to the state before it was updated by the backend
     */
    fun invalidateOnlinePrices() {
        shoppingCartData.value.invalidProducts = null
        shoppingCartData.value.invalidDepositReturnVoucher = false
        shoppingCartData.value.onlineTotalPrice = null

        // reverse-order because we are removing items // Todo: Do we need reverse order???
        shoppingCartData.value.items.forEachIndexed { index, item ->
            if (item.type == ItemType.LINE_ITEM) {
                shoppingCartData.value.items.removeAt(index)
            } else {
                item.lineItem = null
                item.isManualCouponApplied = false
            }
        }
        checkLimits()
        notifyPriceUpdate(this)
    }

    /**
     * Update all prices of the cart
     *
     * @param debounce if set to true delays the updating and batches
     * multiple [.updatePrices] calls together
     */
    fun updatePrices(debounce: Boolean) {
        if (debounce) {
            updater?.dispatchUpdate()
        } else {
            updater?.update(true)
        }
    }

    fun checkForTimeout() {
        val currentTime = System.currentTimeMillis()
        val timeout = instance.config.maxShoppingCartAge
        if (shoppingCartData.value.lastModificationTime + timeout < currentTime) {
            clearBackup()
            invalidate()
        }
    }

    /**
     * Returns the number of times items in the shopping cart were added
     */
    val addCount: Int
        get() = shoppingCartData.value.addCount

    /**
     * Returns the number of times items in the shopping cart were modified
     */
    val modCount: Int
        get() = shoppingCartData.value.modCount

    /**
     * Generate a new uuid.
     *
     *
     * UUID's are used to uniquely identify a specific purchase made by the user. If a new UUID
     * is generated a new checkout can be made.
     *
     *
     * If a checkout already exist with the same UUID, the checkout will get continued.
     */
    fun generateNewUUID() {
        shoppingCartData.value.uuid = UUID.randomUUID().toString()
        notifyProductsUpdate(this)
    }

    /**
     * The UUID of the cart
     *
     *
     * UUID's are used to uniquely identify a specific purchase made by the user. If a new UUID
     * is generated a new checkout can be made.
     *
     *
     * If a checkout already exist with the same UUID, the checkout will get continued.
     */
    val uUID: String
        get() = shoppingCartData.value.uuid

    fun setOnlineTotalPrice(totalPrice: Int) {
        shoppingCartData.value.onlineTotalPrice = totalPrice
    }

    /**
     * Returns true of the carts price is calculated by the backend
     */
    val isOnlinePrice: Boolean
        get() = shoppingCartData.value.onlineTotalPrice != null

    fun setInvalidDepositReturnVoucher(invalidDepositReturnVoucher: Boolean) {
        shoppingCartData.value.invalidDepositReturnVoucher = invalidDepositReturnVoucher
    }

    /**
     * Gets a list of invalid products that were rejected by the backend.
     */
    var invalidProducts: List<Product>?
        get() = shoppingCartData.value.invalidProducts ?: emptyList()
        set(invalidProducts) {
            shoppingCartData.value.invalidProducts = invalidProducts
        }

    fun hasInvalidDepositReturnVoucher(): Boolean {
        return shoppingCartData.value.invalidDepositReturnVoucher
    }

    /**
     * Returns the total price of the cart.
     *
     *
     * If the cart was updated by the backend, the online price is used. If no update was made
     * a locally calculated price will be used
     */
    val totalPrice: Int
        get() {
            shoppingCartData.value.onlineTotalPrice?.let { totalPrice ->
                return totalPrice
            }
            var sum = 0
            shoppingCartData.value.items.forEach {
                sum += it.totalPrice
            }
            sum += totalDepositPrice
            return sum
        }

    /**
     * Returns the total sum of deposit
     */
    val totalDepositPrice: Int
        get() {
            var sum = 0
            var vPOSsum = 0
            shoppingCartData.value.items.forEach { item ->
                if (item.type == ItemType.LINE_ITEM) {
                    vPOSsum += item.totalDepositPrice
                } else {
                    sum += item.totalDepositPrice
                }
            }
            return max(vPOSsum, sum)
        }

    /**
     * The quantity of items in the cart.
     */
    val totalQuantity: Int
        get() {
            var sum = 0
            shoppingCartData.value.items.forEach { item ->
                if (item.type == ItemType.LINE_ITEM) {
                    if (item.lineItem?.type == LineItemType.DEFAULT) {
                        item.lineItem?.let {
                            sum += it.amount
                        }
                    }
                } else if (item.type == ItemType.PRODUCT) {
                    val product = item.product
                    sum += if (product?.type == Product.Type.UserWeighed ||
                        product?.type == Product.Type.PreWeighed ||
                        product?.referenceUnit == Unit.PIECE
                    ) {
                        1
                    } else {
                        item.quantity
                    }
                }
            }
            return sum
        }

    /**
     * Returns true if the shopping cart is over the current set limit
     */
    fun hasReachedMaxCheckoutLimit(): Boolean = shoppingCartData.value.hasRaisedMaxCheckoutLimit

    /**
     * Returns true if the shopping cart is over the current set limit for online checkouts
     */
    fun hasReachedMaxOnlinePaymentLimit(): Boolean = shoppingCartData.value.hasRaisedMaxOnlinePaymentLimit

    private fun updateTimestamp() {
        shoppingCartData.value.lastModificationTime = System.currentTimeMillis()
    }

    fun checkLimits() {
        val totalPrice = totalPrice
        project?.let {
            val checkoutLimit = project.maxCheckoutLimit
            val paymentLimit = project.maxOnlinePaymentLimit
            if (totalPrice < checkoutLimit) {
                shoppingCartData.value.hasRaisedMaxCheckoutLimit = false
            }
            if (totalPrice < paymentLimit) {
                shoppingCartData.value.hasRaisedMaxOnlinePaymentLimit = false
            }
            if (!shoppingCartData.value.hasRaisedMaxCheckoutLimit && checkoutLimit > 0 && totalPrice >= checkoutLimit) {
                shoppingCartData.value.hasRaisedMaxCheckoutLimit = true
                notifyCheckoutLimitReached(this)
            } else if (!shoppingCartData.value.hasRaisedMaxOnlinePaymentLimit && paymentLimit > 0 && totalPrice >= paymentLimit) {
                shoppingCartData.value.hasRaisedMaxOnlinePaymentLimit = true
                notifyOnlinePaymentLimitReached(this)
            }
        }
    }

    /**
     * Returns the current minimum age required to purchase all items of the shopping cart
     */
    val minimumAge: Int
        get() {
            var minimumAge = 0
            shoppingCartData.value.items.forEach {
                minimumAge = max(minimumAge, it.minimumAge)
            }
            return minimumAge
        }

    /**
     * Checks if the provided scanned code is contained inside the shopping cart
     */
    fun containsScannedCode(scannedCode: ScannedCode): Boolean {
        shoppingCartData.value.items.forEach {
            if (it.scannedCode != null && it.scannedCode?.code == scannedCode.code) {
                return true
            }
        }
        return false
    }

    /**
     * Class describing a shopping cart item
     */
    class Item {

        /**
         * Returns the product associated with the shopping cart item.
         */
        var product: Product? = null

        /**
         * Returns the scanned code which was used when scanning the product and adding it to the shopping cart
         */
        var scannedCode: ScannedCode? = null
            private set

        @JvmField
        var quantity = 0

        @JvmField
        var lineItem: LineItem? = null

        /**
         * Returns the id of the shopping cart item
         */
        var id: String? = null
            private set
        private var isUsingSpecifiedQuantity = false

        @Transient
        var cart: ShoppingCart? = null

        @JvmField
        var isManualCouponApplied = false

        @JvmField
        var coupon: Coupon? = null

        // The local generated UUID of a coupon which which will be used by the backend
        var backendCouponId: String? = null

        constructor() {
            // for gson
        }

        constructor(cart: ShoppingCart, coupon: Coupon, scannedCode: ScannedCode?) {
            id = UUID.randomUUID().toString()
            this.cart = cart
            this.scannedCode = scannedCode
            this.coupon = coupon
            backendCouponId = UUID.randomUUID().toString()
        }

        constructor(cart: ShoppingCart, product: Product, scannedCode: ScannedCode) {
            id = UUID.randomUUID().toString()
            this.cart = cart
            this.scannedCode = scannedCode
            this.product = product
            if (product.type == Product.Type.UserWeighed) {
                quantity = 0
            } else {
                product.scannableCodes.forEach { code: Product.Code? ->
                    code ?: return@forEach
                    if (code.template != null && code.template == scannedCode.templateName && code.lookupCode != null && code.lookupCode == scannedCode.lookupCode) {
                        quantity = code.specifiedQuantity
                        if (!code.isPrimary && code.specifiedQuantity > 1) {
                            isUsingSpecifiedQuantity = true
                        }
                    }
                }
                if (quantity == 0) {
                    quantity = 1
                }
            }
            if (scannedCode.hasEmbeddedData() && product.type == Product.Type.DepositReturnVoucher) {
                val builder = scannedCode.newBuilder()
                if (scannedCode.hasEmbeddedData()) {
                    builder.setEmbeddedData(scannedCode.embeddedData * -1)
                }
                if (scannedCode.hasEmbeddedDecimalData()) {
                    builder.setEmbeddedDecimalData(scannedCode.embeddedDecimalData.multiply(BigDecimal(-1)))
                }
                this.scannedCode = builder.create()
            }
        }

        constructor(cart: ShoppingCart, lineItem: LineItem) {
            id = UUID.randomUUID().toString()
            this.cart = cart
            this.lineItem = lineItem
        }

        fun setLineItem(lineItem: LineItem?) {
            this.lineItem = lineItem
        }

        /**
         * Returns the effective quantity (embedded weight OR embedded price)
         * depending on the type
         */
        val effectiveQuantity: Int
            get() = getEffectiveQuantity(false)

        fun getEffectiveQuantity(ignoreLineItem: Boolean): Int {
            val code = scannedCode
            return if (code != null && code.hasEmbeddedData() && code.embeddedData != 0) code.embeddedData else getQuantity(
                ignoreLineItem
            )
        }

        /**
         * Returns the quantity of the cart item
         */
        fun getQuantity(): Int {
            return getQuantity(false)
        }

        /**
         * Returns the quantity of the cart item
         *
         * @param ignoreLineItem if set to true, only return the local quantity before backend updates
         */
        private fun getQuantity(ignoreLineItem: Boolean): Int {
            if (ignoreLineItem) return quantity
            lineItem?.let { item ->
                return item.weight ?: item.units ?: item.amount
            } ?: return quantity
        }

        /**
         * Set the quantity of the cart item
         */
        fun setQuantity(quantity: Int) {
            if (scannedCode?.hasEmbeddedData() == true && scannedCode?.embeddedData != 0) {
                return
            }
            this.quantity = quantity.coerceIn(0, MAX_QUANTITY)
            val index = cart?.shoppingCartData?.value?.items?.indexOf(this)
            if (index != -1 && index != null) {
                cart?.let { currentCart ->
                    if (quantity == 0) {
                        currentCart.shoppingCartData.value.items.remove(this)
                        currentCart.notifyItemRemoved(currentCart, this, index)
                    } else {
                        currentCart.notifyQuantityChanged(currentCart, this)
                    }
                    currentCart.shoppingCartData.value.modCount++
                    currentCart.generateNewUUID()
                    currentCart.invalidateOnlinePrices()
                    currentCart.updatePrices(true)
                }
            }
        }

        /**
         * Returns the [ItemType] of the cart item
         */
        val type: ItemType?
            get() {
                if (product != null) {
                    return ItemType.PRODUCT
                } else if (coupon != null) {
                    return ItemType.COUPON
                } else if (lineItem != null) {
                    return ItemType.LINE_ITEM
                }
                return null
            }

        /**
         * Sets if a manual coupon (coupon applied by the user after scanning) is applied
         */
        fun setManualCouponApplied(manualCouponApplied: Boolean) {
            isManualCouponApplied = manualCouponApplied
        }

        /**
         * Returns true if a manual coupon (coupon applied by the user after scanning) is applied
         */
        fun isManualCouponApplied(): Boolean = isManualCouponApplied

        /**
         * Returns true if the item is editable by the user
         */
        val isEditable: Boolean
            get() = if (coupon != null && coupon?.type != CouponType.MANUAL) {
                false
            } else isEditableInDialog

        /**
         * Returns true if the item is editable by the user while scanning
         */
        val isEditableInDialog: Boolean
            get() = if (lineItem != null) (lineItem?.type == LineItemType.DEFAULT
                    && (scannedCode?.hasEmbeddedData() != true || scannedCode?.embeddedData == 0))
            else (scannedCode?.hasEmbeddedData() != true || scannedCode?.embeddedData == 0) &&
                    product?.getPrice(cart?.project?.customerCardId) != 0

        /**
         * Returns true if the item can be merged with items that conain the same product and type
         */
        val isMergeable: Boolean
            get() {
                val isMergeableOverride = instance.isMergeable
                val isMergeable = isMergeableDefault
                return isMergeableOverride?.isMergeable(this, isMergeable) ?: isMergeable
            }

        private val isMergeableDefault: Boolean
            get() {
                if (product == null && lineItem != null) return false
                return if (coupon != null) false
                else product?.type == Product.Type.Article
                        && unit != Unit.PIECE
                        && product?.getPrice(cart?.project?.customerCardId) != 0
                        && scannedCode?.embeddedData == 0
                        && !isUsingSpecifiedQuantity
            }

        /**
         * Returns the unit associated with the cart item, or null if the cart item has no unit
         */
        val unit: Unit?
            get() {
                if (type == ItemType.PRODUCT) {
                    return scannedCode?.embeddedUnit ?: product?.getEncodingUnit(
                        scannedCode?.templateName,
                        scannedCode?.lookupCode
                    )
                } else if (type == ItemType.LINE_ITEM) {
                    if (lineItem?.weightUnit != null) {
                        return Unit.fromString(lineItem?.weightUnit)
                    }
                }
                return null
            }

        /**
         * Gets the total price of the item
         */
        val totalPrice: Int
            get() = lineItem?.totalPrice ?: localTotalPrice

        /**
         * Gets the total price of the items, ignoring the backend response
         */
        val localTotalPrice: Int
            get() = if (type == ItemType.PRODUCT) {
                if (unit == Unit.PRICE) {
                    scannedCode?.embeddedData
                }
                product?.getPriceForQuantity(
                    effectiveQuantity,
                    scannedCode,
                    cart?.project?.roundingMode,
                    cart?.project?.customerCardId
                ) ?: 0
            } else {
                0
            }

        /**
         * Gets the total deposit of the item
         */
        val totalDepositPrice: Int
            get() {
                val item = lineItem
                if (item != null && item.type === LineItemType.DEPOSIT) {
                    return item.totalPrice
                }
                return if (product != null && product?.depositProduct != null) {
                    val price = product?.depositProduct?.getPrice(cart?.project?.customerCardId) ?: return 0
                    quantity * price
                } else 0
            }

        /**
         * Returns true if the item should be displayed as a discount
         */
        val isDiscount: Boolean
            get() = lineItem != null && lineItem?.type == LineItemType.DISCOUNT

        /**
         * Returns true if the item should be displayed as a giveaway
         */
        val isGiveaway: Boolean
            get() = lineItem != null && lineItem?.type == LineItemType.GIVEAWAY

        /**
         * Gets the price after applying all price modifiers
         */
        val modifiedPrice: Int
            get() {
                var sum = 0
                if (lineItem != null && lineItem?.priceModifiers != null) {
                    lineItem?.priceModifiers?.forEach { priceModifier ->
                        lineItem?.amount?.let {
                            sum += it * priceModifier.price
                        }
                    }
                }
                return sum
            }

        /**
         * Gets the displayed name of the product
         */
        val displayName: String?
            get() = if (lineItem != null) {
                lineItem?.name
            } else {
                if (type == ItemType.COUPON) {
                    coupon?.name
                } else {
                    product?.name
                }
            }

        /**
         * Gets text displaying quantity, can be a weight or price depending on the type
         *
         *
         * E.g. "1" or "100g" or "2,03 €"
         */
        val quantityText: String
            get() {
                if (type == ItemType.LINE_ITEM) {
                    return lineItem?.amount.toString()
                }
                val unit = unit
                val embeddedData = scannedCode?.embeddedData ?: 0
                if (unit == Unit.PRICE || (unit == Unit.PIECE && embeddedData > 0)) {
                    val units = lineItem?.units ?: 1
                    return units.toString()
                }
                val q = effectiveQuantity
                return if (q > 0) {
                    q.toString() + (unit?.displayValue ?: "")
                } else {
                    "1"
                }
            }

        /**
         * Gets text displaying price, including the calculation.
         *
         *
         * E.g. "2 * 3,99 € = 7,98 €"
         */
        val fullPriceText: String?
            get() {
                val priceText = priceText ?: return null
                val quantityText = quantityText
                return if (quantityText == "1") {
                    priceText
                } else {
                    "$quantityText ${this.priceText}"
                }
            }

        private val reducedPriceText: String?
            get() {
                val size = lineItem?.priceModifiers?.size ?: 0
                return if (lineItem?.priceModifiers != null && size > 0) {
                    cart?.priceFormatter?.format(totalPrice, true)
                } else null
            }

        private val extendedPriceText: String?
            get() {
                return reducedPriceText ?: product?.let { product ->
                    lineItem?.let {
                        String.format(
                            Locale.getDefault(),
                            "\u00D7 %s = %s",
                            cart?.priceFormatter?.format(product, it.price),
                            cart?.priceFormatter?.format(totalPrice)
                        )
                    }
                }
            }

        /**
         * Gets the displayed total price
         */
        val totalPriceText: String?
            get() = cart?.priceFormatter?.format(totalPrice)

        /**
         * Gets text displaying price, including the calculation.
         *
         *
         * E.g. "3,99 €" or "2,99 € /kg = 0,47 €"
         */
        val priceText: String?
            get() {
                lineItem?.let {
                    if (it.price != 0) {
                        val units = it.units
                        return if (product != null
                            && units != null
                            && units > 1
                            || unit != Unit.PRICE
                            && (unit != Unit.PIECE || scannedCode?.embeddedData == 0)
                            && effectiveQuantity > 1
                        ) {
                            extendedPriceText
                        } else {
                            if (it.units != null) {
                                extendedPriceText
                            } else {
                                reducedPriceText ?: cart?.priceFormatter?.format(totalPrice, true)
                            }
                        }
                    }
                }
                product ?: return null

                val price = product?.getPrice(cart?.project?.customerCardId) ?: 0
                if (price > 0 || scannedCode?.hasEmbeddedData() == true) {
                    return if ((unit == Unit.PRICE || unit == Unit.PIECE) &&
                        !(scannedCode?.hasEmbeddedData() == true && scannedCode?.embeddedData == 0)
                    ) {
                        cart?.priceFormatter?.format(totalPrice)
                    } else if (effectiveQuantity <= 1) {
                        product?.let {
                            cart?.priceFormatter?.format(it)
                        }
                    } else {
                        product?.let {
                            String.format(
                                Locale.getDefault(),
                                "\u00D7 %s = %s",
                                cart?.priceFormatter?.format(it),
                                cart?.priceFormatter?.format(totalPrice)
                            )
                        }
                    }
                }
                return null
            }

        /**
         * Gets the minimum age required to purchase this item
         */
        val minimumAge: Int
            get() {
                val saleRestriction = product?.saleRestriction ?: return 0
                return if (saleRestriction.isAgeRestriction) {
                    saleRestriction.value.toInt()
                } else 0
            }

        /**
         * Associate a user applied coupon with this item. E.g. manual price reductions.
         */
        fun setCoupon(coupon: Coupon?) {
            if (coupon?.type != CouponType.MANUAL) {
                throw RuntimeException("Only manual coupons can be added")
            }
            this.coupon = coupon
        }

        /**
         * Gets the user associated coupon of this item
         */
        fun getCoupon(): Coupon? = coupon

        fun replace(product: Product?, scannedCode: ScannedCode?, quantity: Int) {
            this.product = product
            this.scannedCode = scannedCode
            this.setQuantity(quantity)
        }
    }

    val availablePaymentMethods: List<PaymentMethodInfo>?
        get() = updater?.lastAvailablePaymentMethods

    val isVerifiedOnline: Boolean
        get() = updater?.isUpdated == true

    fun toJson(): String {
        return GsonHolder.get().toJson(this)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class BackendCart(

        val session: String? = null,

        @SerializedName("shopID")
        val shopId: String? = null,

        @SerializedName("clientID")
        val clientId: String? = null,

        @SerializedName("appUserID")
        val appUserId: String? = null,
        val customer: BackendCartCustomer? = null,

        @JvmField
        val items: List<BackendCartItem> = emptyList(),
        val requiredInformation: MutableList<BackendCartRequiredInformation>? = null,
        override val eventType: EventType = EventType.CART
    ) : Payload

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class BackendCartCustomer(
        val loyaltyCard: String? = null
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    data class BackendCartRequiredInformation(
        var id: String? = null,
        var value: String? = null
    )

    fun createRequiredInformation(): MutableList<BackendCartRequiredInformation>? {
        return if (shoppingCartData.value.taxation != Taxation.UNDECIDED) {
            mutableListOf(
                BackendCartRequiredInformation(
                    id = "taxation",
                    value = shoppingCartData.value.taxation.value
                )
            )
        } else null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBackendCart(): BackendCart {
        val userPreferences = instance.userPreferences
        val loyaltyCardId = project?.customerCardId

        return BackendCart(
            session = id,
            shopId = instance.checkedInShop?.id ?: "unknown",
            clientId = userPreferences.clientId,
            appUserId = userPreferences.appUser?.id,
            customer = if (loyaltyCardId != null) BackendCartCustomer(loyaltyCardId) else null,
            requiredInformation = createRequiredInformation(),
            items = backendCartItems()
        )
    }

    private fun backendCartItems(): MutableList<BackendCartItem> {
        val items: MutableList<BackendCartItem> = ArrayList()
        shoppingCartData.value.items.forEach { cartItem ->
            if (cartItem.type == ItemType.PRODUCT) {
                val product = cartItem.product
                val quantity = cartItem.getQuantity()
                val scannedCode = cartItem.scannedCode
                val encodingUnit = scannedCode?.embeddedUnit ?: product?.getEncodingUnit(
                    scannedCode?.templateName,
                    scannedCode?.lookupCode
                )

                val backendCartItem = BackendCartItem(
                    id = cartItem.id,
                    sku = product?.sku.toString(),
                    scannedCode = getScannedCode(scannedCode, cartItem)
                        ?: product?.primaryCode?.lookupCode
                        ?: scannedCode?.code,
                    weightUnit = encodingUnit?.id,
                    amount = if (product?.type != Product.Type.UserWeighed
                        && cartItem.unit != Unit.PIECE
                        && cartItem.unit != Unit.PRICE
                        && cartItem.unit != null) quantity else 1,
                    units = if (cartItem.unit == Unit.PIECE) cartItem.getEffectiveQuantity(true) else null,
                    price = if (cartItem.unit == Unit.PRICE) cartItem.localTotalPrice
                    else if (scannedCode?.hasPrice() == true) scannedCode.price else null,
                    weight = if (product?.type == Product.Type.UserWeighed) quantity
                    else if (cartItem.unit != Unit.PRICE && cartItem.unit != Unit.PIECE && cartItem.unit != null)
                        cartItem.getEffectiveQuantity(true)
                    else null
                )

                items.add(backendCartItem)
                if (cartItem.coupon != null) {
                    // this item id needs to be unique per item or else the
                    // promotion engine on the backend gets confused
                    val couponItem = BackendCartItem(
                        id = UUID.randomUUID().toString(),
                        refersTo = backendCartItem.id,
                        amount = 1,
                        couponID = cartItem.coupon?.id
                    )
                    items.add(couponItem)
                }
            } else if (cartItem.type == ItemType.COUPON) {
                val item = BackendCartItem(
                    id = cartItem.backendCouponId,
                    amount = 1,
                    scannedCode = cartItem.scannedCode?.code,
                    couponID = cartItem.coupon?.id

                )
                items.add(item)
            }
        }
        return items
    }

    private fun getScannedCode(scannedCode: ScannedCode?, cartItem: Item): String? {
        val templateName = scannedCode?.templateName
        return if (cartItem.unit != Unit.PIECE && scannedCode?.embeddedData != 0 || templateName == null) {
            null
        } else {
            // reencode user input from scanned code with 0 amount
            val codeTemplate = project?.getCodeTemplate(templateName)
            val newCode: ScannedCode? = codeTemplate?.code(scannedCode.lookupCode)
                ?.embed(cartItem.effectiveQuantity)
                ?.buildCode()
            newCode?.code
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun resolveViolations(violations: List<Violation>) {
        violations.forEach { violation ->
            val items = shoppingCartData.value.items.filter { item ->
                item.coupon != null &&
                        item.backendCouponId != null &&
                        item.backendCouponId == violation.refersTo
            }

            items.forEach { item ->
                shoppingCartData.value.items.remove(item)
                shoppingCartData.value.modCount++
                val containsViolation =
                    shoppingCartData.value.violationNotifications.any { it.refersTo == violation.refersTo }
                if (!containsViolation) {
                    shoppingCartData.value.violationNotifications.add(
                        ViolationNotification(
                            item.coupon?.name,
                            violation.refersTo,
                            violation.type,
                            violation.message
                        )
                    )
                }
            }
        }

        notifyViolations()
        updatePrices(false)
    }

    /**
     * Remove the handled ViolationNotifications.
     *
     * @param violations the handled ViolationNotifications.
     */
    fun removeViolationNotification(violations: List<ViolationNotification?>?) {
        violations?.let {
            shoppingCartData.value.violationNotifications.removeAll(violations)
            shoppingCartData.value.modCount++
            notifyCartDataChanged(this)
        }
    }

    val violationNotifications: List<ViolationNotification>
        get() = shoppingCartData.value.violationNotifications

    /**
     * Adds a [ShoppingCartListener] to the list of listeners if it does not already exist.
     *
     * @param listener the listener to addNamedOnly
     */
    fun addListener(listener: ShoppingCartListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        if (shoppingCartData.value.violationNotifications.isNotEmpty()) {
            listener.onViolationDetected(shoppingCartData.value.violationNotifications.toList())
        }
    }

    /**
     * Removes a given [ShoppingCartListener] from the list of listeners.
     *
     * @param listener the listener to remove
     */
    fun removeListener(listener: ShoppingCartListener) {
        listeners.remove(listener)
    }

    private fun notifyItemAdded(list: ShoppingCart, item: Item) {
        updateTimestamp()
        if (list.shoppingCartData.value.items.contains(item)) {
            listeners.forEach {
                it.onItemAdded(list, item)
            }
        }
    }

    private fun notifyItemRemoved(list: ShoppingCart?, item: Item, pos: Int) {
        updateTimestamp()
        Dispatch.mainThread {
            listeners.forEach {
                it.onItemRemoved(list, item, pos)
            }
        }
    }

    private fun notifyQuantityChanged(list: ShoppingCart?, item: Item) {
        updateTimestamp()
        Log.d("xx", "notifyQuantityChanged: ")
        Dispatch.mainThread {
            if (list?.shoppingCartData?.value?.items?.contains(item) == true) {
                Log.d("xx", "notifyQuantityChanged: ")
                listeners.forEach {
                    it.onQuantityChanged(list, item)
                }
            }
        }
    }

    private fun notifyProductsUpdate(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onProductsUpdated(list)
            }
        }
    }

    fun notifyPriceUpdate(list: ShoppingCart?) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onPricesUpdated(list)
            }
        }
    }

    private fun notifyTaxationChanged(list: ShoppingCart, taxation: Taxation) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onTaxationChanged(list, taxation)
            }
        }
    }

    private fun notifyCheckoutLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onCheckoutLimitReached(list)
            }
        }
    }

    private fun notifyOnlinePaymentLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onOnlinePaymentLimitReached(list)
            }
        }
    }

    private fun notifyViolations() {
        Dispatch.mainThread {
            listeners.forEach { listener ->
                shoppingCartData.value.violationNotifications.let {
                    listener.onViolationDetected(it.toList())
                }
            }
        }
    }

    private fun notifyCartDataChanged(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners.forEach {
                it.onCartDataChanged(list)
            }
        }
    }

    /**
     * Notifies all [.listeners] that the shopping list was cleared of all entries.
     *
     * @param list the [ShoppingCart]
     */
    private fun notifyCleared(list: ShoppingCart) {
        updateTimestamp()
        Dispatch.mainThread {
            listeners.forEach {
                it.onCleared(list)
            }
        }
    }

    companion object {

        const val MAX_QUANTITY = 99999
    }
}
