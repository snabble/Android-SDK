package io.snabble.sdk

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.Product.Type
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
import io.snabble.sdk.shoppingcart.data.ItemType
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import java.math.BigDecimal
import java.util.Objects
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.minutes

/**
 * Class representing the snabble shopping cart
 */
class ShoppingCart(
    private val project: Project? = null
) : Iterable<ShoppingCart.Item?> {

    var data: ShoppingCartData = ShoppingCartData()
    private var oldData: ShoppingCartData? = ShoppingCartData()

    @Transient
    private var listeners: MutableList<ShoppingCartListener>? = CopyOnWriteArrayList()

    @Transient
    private var updater: ShoppingCartUpdater? = null

    @Transient
    private var priceFormatter: PriceFormatter? = null

    init {
        updateTimestamp()
        project?.let {
            updater = ShoppingCartUpdater(project, this)
        }
        priceFormatter = project?.priceFormatter
    }

    fun initWithData(data: ShoppingCartData) {
        updateData(data)
        checkForTimeout()
        updatePrices(false)
        notifyCartDataChanged(this)
    }

    private fun updateData(data: ShoppingCartData) {
        this.data = data
        oldData = null
        data.applyShoppingCart(this)
        oldData?.applyShoppingCart(this)
    }

    /**
     * The id used to identify this cart session
     */
    val id: String
        get() = data.id

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

    fun insert(item: Item, index: Int, update: Boolean) {
        val itemIsMerged = insertIfMergeable(item, index, update)
        if (itemIsMerged) return
        data.items.add(index, item)
        clearBackup()
        checkLimits()
        notifyItemAdded(this, item)

        // sort coupons to bottom
        data.items.sortWith(Comparator { o1: Item, o2: Item ->
            val t1 = o1.type
            val t2 = o2.type
            if (t2 == ItemType.COUPON && t1 == ItemType.PRODUCT) {
                return@Comparator -1
            } else if (t1 == ItemType.COUPON && t2 == ItemType.PRODUCT) {
                return@Comparator 1
            } else {
                return@Comparator 0
            }
        })

        if (update) {
            data.addCount++
            data.modCount++
            generateNewUUID()
            invalidateOnlinePrices()
            updatePrices(true)
        }
    }

    private fun insertIfMergeable(item: Item, index: Int, update: Boolean): Boolean {
        return if (item.isMergeable) {
            val existing = getExistingMergeableProduct(item.product) ?: return false
            data.items.remove(existing)
            data.items.add(index, item)
            data.modCount++
            generateNewUUID()
            checkLimits()
            notifyQuantityChanged(this, item)
            if (update) {
                invalidateOnlinePrices()
                updatePrices(true)
            }
            true
        } else false
    }

    /**
     * Returns a cart item that contains the given product, if that cart item
     * can be merged.
     *
     * A cart item is not mergeable if it uses encoded data of a scanned code (e.g. a different price)
     */
    fun getExistingMergeableProduct(product: Product?): Item? {
        product ?: return null
        data.items.forEach { item ->
            if (product == item.product && item.isMergeable) {
                return item
            }
        }
        return null
    }

    /**
     * Gets the cart item a specific index
     */
    operator fun get(index: Int): Item = data.items[index]

    override fun iterator(): MutableIterator<Item> = data.items.iterator()

    /**
     * Find a cart item by it's id
     */
    fun getByItemId(itemId: String?): Item? {
        itemId ?: return null
        data.items.forEach { item ->
            if (itemId == item.id) {
                return item
            }
        }
        return null
    }

    /**
     * Gets the current index of a cart item
     */
    fun indexOf(item: Item?): Int = data.items.indexOf(item)

    /**
     * Removed a cart item from the cart by its index
     */
    fun remove(index: Int) {
        data.modCount++
        generateNewUUID()
        val removedItem = data.items.removeAt(index)
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
    fun size(): Int = data.items.size

    /**
     * Check if the cart is empty
     */
    val isEmpty: Boolean
        get() = data.items.isEmpty()

    /**
     * Backups the cart, so it can be restured using [.restore] later.
     *
     *
     * A cart is restorable for up to 5 minutes.
     */
    fun backup() {
        if (data.items.size > 0) {
            oldData = data.deepCopy()
            data.backupTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Check if the cart is backed up by [.backup] and still in the 5 minute time window
     */
    val isRestorable: Boolean
        get() = oldData != null && data.backupTimestamp > System.currentTimeMillis() - 5.minutes.inWholeMilliseconds

    /**
     * Clears the backup storage of the cart
     */
    fun clearBackup() {
        oldData = null
        data.backupTimestamp = 0
    }

    /**
     * Restores the cart previously backed up by [.backup]
     */
    fun restore() {
        val restorableData = oldData ?: return
        if (isRestorable) {
            data = restorableData
            data.applyShoppingCart(this)
            clearBackup()
            checkLimits()
            updatePrices(false)
            notifyProductsUpdate(this)
        }
    }

    /**
     * The last time the cart was backed up by using [.backup]
     *
     * @return
     */
    val backupTimestamp: Long
        get() = data.backupTimestamp

    /**
     * Clears the cart of all items
     */
    fun clear() {
        data.items = mutableListOf()
        data.modCount = 0
        data.addCount = 0
        generateNewUUID()
        data.onlineTotalPrice = null
        checkLimits()
        updatePrices(false)
        notifyCleared(this)
    }

    var taxation: Taxation
        /**
         * Gets the current [Taxation] type of the shopping cart
         */
        get() = data.taxation
        /**
         * Sets the current taxation type of the cart
         */
        set(taxation) {
            data.taxation = taxation
            notifyTaxationChanged(this, taxation)
        }

    /**
     * Clears the cart and generated a cart new session.
     */
    fun invalidate() {
        data.id = UUID.randomUUID().toString()
        generateNewUUID()
        clear()
    }

    /**
     * Updates each items products in the shopping cart
     */
    fun updateProducts() {
        val productDatabase = project?.productDatabase
        if (productDatabase?.isUpToDate == true) {
            data.items.forEach { item ->
                val product = productDatabase.findByCode(item.scannedCode) ?: return@forEach
                item.product = product

            }
            notifyProductsUpdate(this)
        }
        updatePrices(false)
    }

    /**
     * Resets the cart to the state before it was updated by the backend
     */
    fun invalidateOnlinePrices() {
        data.invalidProducts = null
        data.invalidDepositReturnVoucher = false
        data.onlineTotalPrice = null

        data.items.forEach { item ->
            if (item.type == ItemType.LINE_ITEM) {
                data.items.remove(item)
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
        if (data.lastModificationTime + timeout < currentTime) {
            clearBackup()
            invalidate()
        }
    }

    /**
     * Returns the number of times items in the shopping cart were added
     */
    val addCount: Int
        get() = data.addCount

    /**
     * Returns the number of times items in the shopping cart were modified
     */
    val modCount: Int
        get() = data.modCount

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
        data.uuid = UUID.randomUUID().toString()
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
        get() = data.uuid

    fun setOnlineTotalPrice(totalPrice: Int) {
        data.onlineTotalPrice = totalPrice
    }

    /**
     * Returns true of the carts price is calculated by the backend
     */
    val isOnlinePrice: Boolean
        get() = data.onlineTotalPrice != null

    fun setInvalidDepositReturnVoucher(invalidDepositReturnVoucher: Boolean) {
        data.invalidDepositReturnVoucher = invalidDepositReturnVoucher
    }

    /**
     * Gets a list of invalid products that were rejected by the backend.
     */
    var invalidProducts: List<Product>?
        get() = data.invalidProducts ?: emptyList()
        set(invalidProducts) {
            data.invalidProducts = invalidProducts
        }

    fun hasInvalidDepositReturnVoucher(): Boolean = data.invalidDepositReturnVoucher

    /**
     * Returns the total price of the cart.
     *
     * If the cart was updated by the backend, the online price is used. If no update was made
     * a locally calculated price will be used
     */
    val totalPrice: Int
        get() = data.onlineTotalPrice ?: calculateTotalPrice()

    private fun calculateTotalPrice(): Int {
        var sum = 0
        data.items.forEach { item ->
            sum += item.totalPrice
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
            data.items.forEach { item ->
                if (item.type == ItemType.LINE_ITEM) {
                    vPOSsum += item.totalDepositPrice
                } else {
                    sum += item.totalDepositPrice
                }
            }

            return vPOSsum.coerceAtLeast(sum)
        }

    /**
     * The quantity of items in the cart.
     */
    val totalQuantity: Int
        get() {
            var sum = 0
            data.items.forEach { item ->
                if (item.type == ItemType.LINE_ITEM) {
                    val lineItem = item.lineItem ?: return@forEach
                    if (item.lineItem?.type == LineItemType.DEFAULT) {
                        sum += lineItem.amount
                    }
                } else if (item.type == ItemType.PRODUCT) {
                    val product = item.product
                    val weightedTypes = listOf(Type.UserWeighed, Type.PreWeighed)
                    sum += if (weightedTypes.contains(product?.type) || product?.referenceUnit == Unit.PIECE) {
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
    fun hasReachedMaxCheckoutLimit(): Boolean = data.hasRaisedMaxCheckoutLimit

    /**
     * Returns true if the shopping cart is over the current set limit for online checkouts
     */
    fun hasReachedMaxOnlinePaymentLimit(): Boolean = data.hasRaisedMaxOnlinePaymentLimit

    private fun updateTimestamp() {
        data.lastModificationTime = System.currentTimeMillis()
    }

    fun checkLimits() {
        val totalPrice = totalPrice
        val maxCheckoutLimit = project?.maxCheckoutLimit ?: 0
        val maxOnlinePaymentLimit = project?.maxOnlinePaymentLimit ?: 0
        if (totalPrice < maxCheckoutLimit) {
            data.hasRaisedMaxCheckoutLimit = false
        }
        if (totalPrice < maxOnlinePaymentLimit) {
            data.hasRaisedMaxOnlinePaymentLimit = false
        }
        if (!data.hasRaisedMaxCheckoutLimit
            && maxCheckoutLimit > 0
            && totalPrice >= maxCheckoutLimit
        ) {
            data.hasRaisedMaxCheckoutLimit = true
            notifyCheckoutLimitReached(this)
        } else if (!data.hasRaisedMaxOnlinePaymentLimit
            && maxOnlinePaymentLimit > 0
            && totalPrice >= maxOnlinePaymentLimit
        ) {
            data.hasRaisedMaxOnlinePaymentLimit = true
            notifyOnlinePaymentLimitReached(this)
        }
    }

    /**
     * Returns the current minimum age required to purchase all items of the shopping cart
     */
    val minimumAge: Int
        get() {
            var minimumAge = 0
            data.items.forEach { item ->
                minimumAge = minimumAge.coerceAtLeast(item.minimumAge)
            }
            return minimumAge
        }

    /**
     * Checks if the provided scanned code is contained inside the shopping cart
     */
    fun containsScannedCode(scannedCode: ScannedCode): Boolean {
        data.items.forEach { item ->
            if (item.scannedCode != null && item.scannedCode?.code == scannedCode.code) {
                return true
            }

        }
        return false
    }

    class CouponItem(val coupon: Coupon, val scannedCode: ScannedCode) {

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as CouponItem
            return coupon == that.coupon && scannedCode == that.scannedCode
        }

        override fun hashCode(): Int {
            return Objects.hash(coupon, scannedCode)
        }
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

        //helper
        var quantity = 0

        var lineItem: LineItem? = null

        /**
         * Returns the id of the shopping cart item
         */
        var id: String? = null
            private set
        private var isUsingSpecifiedQuantity = false

        @Transient
        var cart: ShoppingCart? = null
        var isManualCouponApplied = false
        var coupon: Coupon? = null

        // The local generated UUID of a coupon which which will be used by the backend
        var backendCouponId: String? = null

        protected constructor() {
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
                for (code in product.scannableCodes) {
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

        fun setLineItem2(lineItem: LineItem?) {
            this.lineItem = lineItem
        }

        /**
         * Returns the effective quantity (embedded weight OR embedded price)
         * depending on the type
         */
        val effectiveQuantity: Int
            get() = getEffectiveQuantity(false)

        fun getEffectiveQuantity(ignoreLineItem: Boolean): Int {
            return if (scannedCode != null && scannedCode!!.hasEmbeddedData() && scannedCode!!.embeddedData != 0) scannedCode!!.embeddedData else getQuantityMethod(
                ignoreLineItem
            )
        }

        /**
         * Returns the quantity of the cart item
         */
        fun getQuantityMethod(): Int {
            return getQuantityMethod(false)
        }

        /**
         * Returns the quantity of the cart item
         *
         * @param ignoreLineItem if set to true, only return the local quantity before backend updates
         */
        fun getQuantityMethod(ignoreLineItem: Boolean): Int {
            return if (lineItem != null && !ignoreLineItem) {
                if (lineItem!!.weight != null) {
                    lineItem!!.weight!!
                } else if (lineItem!!.units != null) {
                    lineItem!!.units!!
                } else {
                    lineItem!!.amount
                }
            } else quantity
        }

        /**
         * Set the quantity of the cart item
         */
        fun setQuantityMethod(quantity: Int) {
            if (scannedCode!!.hasEmbeddedData() && scannedCode!!.embeddedData != 0) {
                return
            }
            this.quantity = Math.max(0, Math.min(MAX_QUANTITY, quantity))
            val index = cart!!.data.items.indexOf(this)
            if (index != -1) {
                if (quantity == 0) {
                    cart!!.data.items.remove(this)
                    cart!!.notifyItemRemoved(cart, this, index)
                } else {
                    cart!!.notifyQuantityChanged(cart, this)
                }
                cart!!.data.modCount++
                cart!!.generateNewUUID()
                cart!!.invalidateOnlinePrices()
                cart!!.updatePrices(true)
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
        fun setManualCouponApplied2(manualCouponApplied: Boolean) {
            isManualCouponApplied = manualCouponApplied
        }

        /**
         * Returns true if a manual coupon (coupon applied by the user after scanning) is applied
         */
        fun isManualCouponApplied2(): Boolean {
            return isManualCouponApplied
        }

        /**
         * Returns true if the item is editable by the user
         */
        val isEditable: Boolean
            get() = if (coupon != null && coupon!!.type !== CouponType.MANUAL) {
                false
            } else isEditableInDialog

        /**
         * Returns true if the item is editable by the user while scanning
         */
        val isEditableInDialog: Boolean
            get() = if (lineItem != null) (lineItem!!.type === LineItemType.DEFAULT
                    && (!scannedCode!!.hasEmbeddedData() || scannedCode!!.embeddedData == 0)) else (!scannedCode!!.hasEmbeddedData() || scannedCode!!.embeddedData == 0) &&
                    product!!.getPrice(cart!!.project!!.customerCardId) != 0

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
            private get() {
                if (product == null && lineItem != null) return false
                return if (coupon != null) false else product!!.type == Product.Type.Article && unit != Unit.PIECE && product!!.getPrice(
                    cart!!.project!!.customerCardId
                ) != 0 && scannedCode!!.embeddedData == 0 && !isUsingSpecifiedQuantity
            }

        /**
         * Returns the unit associated with the cart item, or null if the cart item has no unit
         */
        val unit: Unit?
            get() {
                if (type == ItemType.PRODUCT) {
                    return if (scannedCode!!.embeddedUnit != null) scannedCode!!.embeddedUnit else product!!.getEncodingUnit(
                        scannedCode!!.templateName, scannedCode!!.lookupCode
                    )
                } else if (type == ItemType.LINE_ITEM) {
                    if (lineItem!!.weightUnit != null) {
                        return Unit.fromString(lineItem!!.weightUnit)
                    }
                }
                return null
            }

        /**
         * Gets the total price of the item
         */
        val totalPrice: Int
            get() = if (lineItem != null) {
                lineItem!!.totalPrice
            } else localTotalPrice

        /**
         * Gets the total price of the items, ignoring the backend response
         */
        val localTotalPrice: Int
            get() = if (type == ItemType.PRODUCT) {
                if (unit == Unit.PRICE) {
                    scannedCode!!.embeddedData
                }
                product!!.getPriceForQuantity(
                    effectiveQuantity,
                    scannedCode,
                    cart!!.project!!.roundingMode,
                    cart!!.project!!.customerCardId
                )
            } else {
                0
            }

        /**
         * Gets the total deposit of the item
         */
        val totalDepositPrice: Int
            get() {
                if (lineItem != null && lineItem!!.type === LineItemType.DEPOSIT) {
                    return lineItem!!.totalPrice
                }
                return if (product != null && product!!.depositProduct != null) {
                    quantity * product!!.depositProduct!!.getPrice(cart!!.project!!.customerCardId)
                } else 0
            }

        /**
         * Returns true if the item should be displayed as a discount
         */
        val isDiscount: Boolean
            get() = lineItem != null && lineItem!!.type === LineItemType.DISCOUNT

        /**
         * Returns true if the item should be displayed as a giveaway
         */
        val isGiveaway: Boolean
            get() = lineItem != null && lineItem!!.type === LineItemType.GIVEAWAY

        /**
         * Gets the price after applying all price modifiers
         */
        val modifiedPrice: Int
            get() {
                var sum = 0
                if (lineItem != null && lineItem!!.priceModifiers != null) {
                    for ((_, price) in lineItem!!.priceModifiers!!) {
                        sum += lineItem!!.amount * price
                    }
                }
                return sum
            }

        /**
         * Gets the displayed name of the product
         */
        val displayName: String?
            get() = if (lineItem != null) {
                lineItem!!.name
            } else {
                if (type == ItemType.COUPON) {
                    coupon!!.name
                } else {
                    product!!.name
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
                    return lineItem!!.amount.toString()
                }
                val unit = unit
                if ((unit == Unit.PRICE || unit == Unit.PIECE) && scannedCode!!.embeddedData > 0) {
                    return if (lineItem != null && lineItem!!.units != null) {
                        lineItem!!.units.toString()
                    } else {
                        "1"
                    }
                }
                val q = effectiveQuantity
                return if (q > 0) {
                    q.toString() + if (unit != null) unit.displayValue else ""
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
                val priceText = priceText
                if (priceText != null) {
                    val quantityText = quantityText
                    return if (quantityText == "1") {
                        this.priceText
                    } else {
                        quantityText + " " + this.priceText
                    }
                }
                return null
            }
        private val reducedPriceText: String?
            private get() = if (lineItem!!.priceModifiers != null && lineItem!!.priceModifiers!!.size > 0) {
                cart!!.priceFormatter!!.format(totalPrice, true)
            } else null
        private val extendedPriceText: String?
            private get() {
                val reducedPriceText = reducedPriceText
                return if (reducedPriceText != null) {
                    this.reducedPriceText
                } else {
                    String.format(
                        "\u00D7 %s = %s",
                        cart!!.priceFormatter!!.format(product!!, lineItem!!.price),
                        cart!!.priceFormatter!!.format(totalPrice)
                    )
                }
            }

        /**
         * Gets the displayed total price
         */
        val totalPriceText: String
            get() = cart!!.priceFormatter!!.format(totalPrice)

        /**
         * Gets text displaying price, including the calculation.
         *
         *
         * E.g. "3,99 €" or "2,99 € /kg = 0,47 €"
         */
        val priceText: String?
            get() {
                if (lineItem != null) {
                    if (lineItem!!.price != 0) {
                        return if (product != null && lineItem!!.units != null && lineItem!!.units!! > 1 || unit != Unit.PRICE && (unit != Unit.PIECE || scannedCode!!.embeddedData == 0) && effectiveQuantity > 1) {
                            extendedPriceText
                        } else {
                            if (lineItem!!.units != null) {
                                extendedPriceText
                            } else {
                                val reducedPriceText = reducedPriceText
                                reducedPriceText ?: cart!!.priceFormatter!!.format(totalPrice, true)
                            }
                        }
                    }
                }
                if (product == null) {
                    return null
                }
                if (product!!.getPrice(cart!!.project!!.customerCardId) > 0 || scannedCode!!.hasEmbeddedData()) {
                    val unit = unit
                    return if ((unit == Unit.PRICE || unit == Unit.PIECE) && !(scannedCode!!.hasEmbeddedData() && scannedCode!!.embeddedData == 0)) {
                        cart!!.priceFormatter!!.format(totalPrice)
                    } else if (effectiveQuantity <= 1) {
                        cart!!.priceFormatter!!.format(product!!)
                    } else {
                        String.format(
                            "\u00D7 %s = %s",
                            cart!!.priceFormatter!!.format(product!!),
                            cart!!.priceFormatter!!.format(totalPrice)
                        )
                    }
                }
                return null
            }

        /**
         * Gets the minimum age required to purchase this item
         */
        val minimumAge: Int
            get() {
                if (product != null) {
                    val saleRestriction = product!!.saleRestriction
                    if (saleRestriction != null && saleRestriction.isAgeRestriction) {
                        return saleRestriction.value.toInt()
                    }
                }
                return 0
            }

        /**
         * Associate a user applied coupon with this item. E.g. manual price reductions.
         */
        fun setCoupon2(coupon: Coupon?) {
            if (coupon == null) {
                this.coupon = null
                return
            }
            if (coupon.type !== CouponType.MANUAL) {
                throw RuntimeException("Only manual coupons can be added")
            }
            this.coupon = coupon
        }

        /**
         * Gets the user associated coupon of this item
         */
        fun getCoupon2(): Coupon? {
            return coupon
        }

        fun replace(product: Product?, scannedCode: ScannedCode?, quantity: Int) {
            this.product = product
            this.scannedCode = scannedCode
            this.quantity = quantity
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
    class BackendCart : Payload {

        var session: String? = null

        @SerializedName("shopID")
        var shopId: String? = null

        @SerializedName("clientID")
        var clientId: String? = null

        @SerializedName("appUserID")
        var appUserId: String? = null
        var customer: BackendCartCustomer? = null
        lateinit var items: Array<BackendCartItem>
        var requiredInformation: MutableList<BackendCartRequiredInformation>? = null
        override val eventType: EventType
            get() = EventType.CART
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class BackendCartCustomer {

        var loyaltyCard: String? = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class BackendCartRequiredInformation {

        var id: String? = null
        var value: String? = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class BackendCartItem {

        var id: String? = null
        var sku: String? = null

        @JvmField
        var scannedCode: String? = null

        @JvmField
        var amount = 0

        @JvmField
        var weightUnit: String? = null

        @JvmField
        var price: Int? = null

        @JvmField
        var weight: Int? = null

        @JvmField
        var units: Int? = null
        var refersTo: String? = null
        var couponID: String? = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBackendCart(): BackendCart {
        val backendCart = BackendCart()
        backendCart.session = id
        backendCart.shopId = "unknown"
        val userPreferences = instance.userPreferences
        backendCart.clientId = userPreferences.clientId
        val appUser = userPreferences.appUser
        if (appUser != null) {
            backendCart.appUserId = appUser.id
        }
        val loyaltyCardId = project!!.customerCardId
        if (loyaltyCardId != null) {
            backendCart.customer = BackendCartCustomer()
            backendCart.customer!!.loyaltyCard = loyaltyCardId
        }
        if (backendCart.requiredInformation == null) {
            backendCart.requiredInformation = ArrayList()
        }
        if (data.taxation != Taxation.UNDECIDED) {
            val requiredInformation = BackendCartRequiredInformation()
            requiredInformation.id = "taxation"
            requiredInformation.value = data.taxation.value
            backendCart.requiredInformation!!.add(requiredInformation)
        }
        val shop = instance.checkedInShop
        if (shop != null) {
            val id = shop.id
            if (id != null) {
                backendCart.shopId = id
            }
        }
        val items: MutableList<BackendCartItem> = ArrayList()
        for (i in 0 until size()) {
            val cartItem = get(i)
            if (cartItem.type == ItemType.PRODUCT) {
                val item = BackendCartItem()
                val product = cartItem.product
                val quantity = cartItem.getQuantityMethod()
                val scannedCode = cartItem.scannedCode
                var encodingUnit = product!!.getEncodingUnit(scannedCode!!.templateName, scannedCode.lookupCode)
                if (scannedCode.embeddedUnit != null) {
                    encodingUnit = scannedCode.embeddedUnit
                }
                item.id = cartItem.id
                item.sku = product.sku.toString()
                item.scannedCode = scannedCode.code
                if (product.primaryCode != null) {
                    item.scannedCode = product.primaryCode.lookupCode
                }
                if (encodingUnit != null) {
                    item.weightUnit = encodingUnit.id
                }
                item.amount = 1
                if (cartItem.unit == Unit.PIECE) {
                    item.units = cartItem.getEffectiveQuantity(true)
                } else if (cartItem.unit == Unit.PRICE) {
                    item.price = cartItem.localTotalPrice
                } else if (cartItem.unit != null) {
                    item.weight = cartItem.getEffectiveQuantity(true)
                } else if (product.type == Product.Type.UserWeighed) {
                    item.weight = quantity
                } else {
                    item.amount = quantity
                }
                if (item.price == null && scannedCode.hasPrice()) {
                    item.price = scannedCode.price
                }

                // reencode user input from scanned code with 0 amount
                if (cartItem.unit == Unit.PIECE && scannedCode.embeddedData == 0) {
                    val codeTemplate = project!!.getCodeTemplate(scannedCode.templateName!!)
                    if (codeTemplate != null) {
                        val newCode = codeTemplate.code(scannedCode.lookupCode)
                            .embed(cartItem.effectiveQuantity)
                            .buildCode()
                        if (newCode != null) {
                            item.scannedCode = newCode.code
                        }
                    }
                }
                items.add(item)
                if (cartItem.coupon != null) {
                    val couponItem = BackendCartItem()
                    // this item id needs to be unique per item or else the
                    // promotion engine on the backend gets confused
                    couponItem.id = UUID.randomUUID().toString()
                    couponItem.refersTo = item.id
                    couponItem.amount = 1
                    couponItem.couponID = cartItem.coupon!!.id
                    items.add(couponItem)
                }
            } else if (cartItem.type == ItemType.COUPON) {
                val item = BackendCartItem()
                item.id = cartItem.backendCouponId
                item.amount = 1
                val scannedCode = cartItem.scannedCode
                if (scannedCode != null) {
                    item.scannedCode = scannedCode.code
                }
                item.couponID = cartItem.coupon!!.id
                items.add(item)
            }
        }
        backendCart.items = items.toTypedArray()
        return backendCart
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun resolveViolations(violations: List<Violation>) {
        for ((type, refersTo, message) in violations) {
            for (i in data.items.indices.reversed()) {
                val item = data.items[i]
                if (item.coupon != null && item.backendCouponId != null && item.backendCouponId == refersTo) {
                    data.items.remove(item)
                    data.modCount++
                    var found = false
                    for ((_, refersTo1) in data.violationNotifications) {
                        if (refersTo1 == refersTo) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        data.violationNotifications.add(
                            ViolationNotification(
                                item.coupon!!.name,
                                refersTo,
                                type,
                                message
                            )
                        )
                    }
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
        violations ?: return
        data.violationNotifications.removeAll(violations)
        data.modCount++
        notifyCartDataChanged(this)
    }

    val violationNotifications: List<ViolationNotification>
        get() = data.violationNotifications

    /**
     * Adds a [ShoppingCartListener] to the list of listeners if it does not already exist.
     *
     * @param listener the listener to addNamedOnly
     */
    fun addListener(listener: ShoppingCartListener) {
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
        if (!data.violationNotifications.isEmpty()) {
            listener.onViolationDetected(data.violationNotifications)
        }
    }

    /**
     * Removes a given [ShoppingCartListener] from the list of listeners.
     *
     * @param listener the listener to remove
     */
    fun removeListener(listener: ShoppingCartListener) {
        listeners!!.remove(listener)
    }

    /**
     * Shopping list listener that detects various changes to the shopping list.
     */
    interface ShoppingCartListener {

        fun onItemAdded(list: ShoppingCart?, item: Item?)
        fun onQuantityChanged(list: ShoppingCart?, item: Item?)
        fun onCleared(list: ShoppingCart?)
        fun onItemRemoved(list: ShoppingCart?, item: Item?, pos: Int)
        fun onProductsUpdated(list: ShoppingCart?)
        fun onPricesUpdated(list: ShoppingCart?)
        fun onCheckoutLimitReached(list: ShoppingCart?)
        fun onOnlinePaymentLimitReached(list: ShoppingCart?)
        fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?)
        fun onViolationDetected(violations: List<ViolationNotification?>)
        fun onCartDataChanged(list: ShoppingCart?)
    }

    abstract class SimpleShoppingCartListener : ShoppingCartListener {

        abstract fun onChanged(list: ShoppingCart?)
        override fun onProductsUpdated(list: ShoppingCart?) {
            onChanged(list)
        }

        override fun onItemAdded(list: ShoppingCart?, item: Item?) {
            onChanged(list)
        }

        override fun onQuantityChanged(list: ShoppingCart?, item: Item?) {
            onChanged(list)
        }

        override fun onCleared(list: ShoppingCart?) {
            onChanged(list)
        }

        override fun onItemRemoved(list: ShoppingCart?, item: Item?, pos: Int) {
            onChanged(list)
        }

        override fun onPricesUpdated(list: ShoppingCart?) {
            onChanged(list)
        }

        override fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?) {
            onChanged(list)
        }

        override fun onCheckoutLimitReached(list: ShoppingCart?) {}
        override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {}
        override fun onViolationDetected(violations: List<ViolationNotification?>) {}
        override fun onCartDataChanged(list: ShoppingCart?) {
            onChanged(list)
        }
    }

    private fun notifyItemAdded(list: ShoppingCart, item: Item) {
        updateTimestamp()
        Dispatch.mainThread {
            if (list.data.items.contains(item)) {
                for (listener in listeners!!) {
                    listener.onItemAdded(list, item)
                }
            }
        }
    }

    private fun notifyItemRemoved(list: ShoppingCart?, item: Item, pos: Int) {
        updateTimestamp()
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onItemRemoved(list, item, pos)
            }
        }
    }

    private fun notifyQuantityChanged(list: ShoppingCart?, item: Item) {
        updateTimestamp()
        Dispatch.mainThread {
            if (list!!.data.items.contains(item)) {
                for (listener in listeners!!) {
                    listener.onQuantityChanged(list, item)
                }
            }
        }
    }

    private fun notifyProductsUpdate(list: ShoppingCart) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onProductsUpdated(list)
            }
        }
    }

    fun notifyPriceUpdate(list: ShoppingCart?) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onPricesUpdated(list)
            }
        }
    }

    private fun notifyTaxationChanged(list: ShoppingCart, taxation: Taxation) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onTaxationChanged(list, taxation)
            }
        }
    }

    private fun notifyCheckoutLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onCheckoutLimitReached(list)
            }
        }
    }

    private fun notifyOnlinePaymentLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onOnlinePaymentLimitReached(list)
            }
        }
    }

    private fun notifyViolations() {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onViolationDetected(data.violationNotifications)
            }
        }
    }

    private fun notifyCartDataChanged(list: ShoppingCart) {
        Dispatch.mainThread {
            for (listener in listeners!!) {
                listener.onCartDataChanged(list)
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
            for (listener in listeners!!) {
                listener.onCleared(list)
            }
        }
    }

    companion object {

        const val MAX_QUANTITY = 99999
    }
}
