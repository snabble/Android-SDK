package io.snabble.sdk.shoppingcart

import androidx.annotation.RestrictTo
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Product
import io.snabble.sdk.Product.Type
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.Unit
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.checkout.PaymentMethodInfo
import io.snabble.sdk.checkout.Violation
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.shoppingcart.data.cart.BackendCart
import io.snabble.sdk.shoppingcart.data.cart.BackendCartCustomer
import io.snabble.sdk.shoppingcart.data.cart.BackendCartRequiredInformation
import io.snabble.sdk.shoppingcart.data.item.BackendCartItem
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import java.math.BigDecimal
import java.util.Locale
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
    private val listeners: MutableList<ShoppingCartListener>? = CopyOnWriteArrayList()

    @Transient
    private var updater: ShoppingCartUpdater? = null

    @Transient
    private var priceFormatter: PriceFormatter? = null

    init {
        updateTimestamp()
        updater = project?.let { ShoppingCartUpdater(it, this) }
        priceFormatter = project?.priceFormatter
    }

    fun initWithData(data: ShoppingCartData) {
        updateData(data)
        checkForTimeout()
        updatePrices(debounce = false)
        notifyCartDataChanged(list = this)
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
    fun insert(item: Item, index: Int) = insert(item, index, update = true)

    fun insert(item: Item, index: Int, update: Boolean) {
        val itemIsMerged = insertIfMergeable(item, index, update)
        if (itemIsMerged) return
        data.items.add(index, item)
        clearBackup()
        checkLimits()
        notifyItemAdded(this, item)

        sortCouponsToBottom()

        if (update) {
            data = data.copy(addCount = addCount.inc(), modCount = modCount.inc())
            generateNewUUID()
            invalidateOnlinePrices()
            updatePrices(debounce = true)
        }
    }

    private fun sortCouponsToBottom() {
        data.items.sortWith(Comparator { item: Item, otherItem: Item ->
            val itemType = item.type
            val otherItemType = otherItem.type
            if (itemType == ItemType.PRODUCT && otherItemType == ItemType.COUPON) {
                return@Comparator -1
            } else if (itemType == ItemType.COUPON && otherItemType == ItemType.PRODUCT) {
                return@Comparator 1
            } else {
                return@Comparator 0
            }
        })
    }

    private fun insertIfMergeable(item: Item, index: Int, update: Boolean): Boolean {
        return if (item.isMergeable) {
            val existing = getExistingMergeableProduct(item.product) ?: return false
            data.items.remove(existing)
            data.items.add(index, item)
            data = data.copy(modCount = modCount.inc())
            generateNewUUID()
            checkLimits()
            notifyQuantityChanged(this, item)
            if (update) {
                invalidateOnlinePrices()
                updatePrices(debounce = true)
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
    fun getExistingMergeableProduct(product: Product?): Item? =
        data.items.firstOrNull { product == it.product && it.isMergeable }

    /**
     * Gets the cart item a specific index
     */
    operator fun get(index: Int): Item = data.items[index]

    override fun iterator(): MutableIterator<Item> = data.items.iterator()

    /**
     * Find a cart item by it's id
     */
    fun getByItemId(itemId: String?): Item? = data.items.firstOrNull { itemId == it.id }

    /**
     * Gets the current index of a cart item
     */
    fun indexOf(item: Item?): Int = data.items.indexOf(item)

    /**
     * Removed a cart item from the cart by its index
     */
    fun remove(index: Int) {
        data = data.copy(modCount = modCount.inc())
        generateNewUUID()
        val removedItem = data.items.removeAt(index)
        checkLimits()
        updatePrices(debounce = size() != 0)
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
            data = data.copy(backupTimestamp = System.currentTimeMillis())
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
        data = data.copy(backupTimestamp = 0)
    }

    /**
     * Restores the cart previously backed up by [.backup]
     */
    fun restore() {
        if (isRestorable) {
            oldData?.let { data = it }
            data.applyShoppingCart(this)
            clearBackup()
            checkLimits()
            updatePrices(debounce = false)
            notifyProductsUpdate(list = this)
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
        data = data.copy(
            items = mutableListOf(),
            modCount = 0,
            addCount = 0,
            onlineTotalPrice = null
        )
        generateNewUUID()
        checkLimits()
        updatePrices(debounce = false)
        notifyCleared(list = this)
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
            data = data.copy(taxation = taxation)
            notifyTaxationChanged(this, taxation)
        }

    /**
     * Clears the cart and generated a cart new session.
     */
    fun invalidate() {
        data = data.copy(id = UUID.randomUUID().toString())
        generateNewUUID()
        clear()
    }

    /**
     * Updates each items products in the shopping cart
     */
    fun updateProducts() {
        val productDatabase = project?.productDatabase
        if (productDatabase?.isUpToDate == true) {
            data.items.replaceAll { item ->
                val product: Product? = productDatabase.findByCode(item.scannedCode)
                item.apply { product?.let { item.product = product } }
            }

            notifyProductsUpdate(this)
        }
        updatePrices(debounce = false)
    }

    /**
     * Resets the cart to the state before it was updated by the backend
     */
    fun invalidateOnlinePrices() {
        data = data.copy(
            invalidProducts = null,
            invalidDepositReturnVoucher = false,
            onlineTotalPrice = null
        )

        // use iterator directly instead of for each loop to avoid
        // ConcurrentModificationException due to modification while looping
        val iterator: MutableIterator<Item> = iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            when (item.type) {
                ItemType.LINE_ITEM -> iterator.remove()

                else -> {
                    item.lineItem = null
                    item.isManualCouponApplied = false
                }
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
            updater?.update(force = true)
        }
    }

    fun checkForTimeout() {
        val currentTime = System.currentTimeMillis()
        val timeout = Snabble.config.maxShoppingCartAge
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
        data = data.copy(uuid = UUID.randomUUID().toString())
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
    val uuid: String
        get() = data.uuid

    fun setOnlineTotalPrice(totalPrice: Int) {
        data = data.copy(onlineTotalPrice = totalPrice)
    }

    /**
     * Returns true of the carts price is calculated by the backend
     */
    val isOnlinePrice: Boolean
        get() = data.onlineTotalPrice != null

    fun setInvalidDepositReturnVoucher(invalidDepositReturnVoucher: Boolean) {
        data = data.copy(invalidDepositReturnVoucher = invalidDepositReturnVoucher)
    }

    fun isCouponApplied(coupon: Coupon): Boolean = any { it?.coupon?.id == coupon.id }

    fun removeCoupon(coupon: Coupon) {
        val index = indexOfFirst { it?.coupon?.id == coupon.id }
        if (index != -1) remove(index)
    }

    /**
     * Gets a list of invalid products that were rejected by the backend.
     */
    var invalidProducts: List<Product>?
        get() = data.invalidProducts ?: emptyList()
        set(invalidProducts) {
            data = data.copy(invalidProducts = invalidProducts)
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

    private fun calculateTotalPrice(): Int = data.items.sumOf { it.totalPrice } + totalDepositPrice

    /**
     * Returns the total sum of deposit
     */
    val totalDepositPrice: Int
        get() {
            var sum = 0
            var vPosSum = 0
            data.items.forEach { item ->
                if (item.type == ItemType.LINE_ITEM) {
                    vPosSum += item.totalDepositPrice
                } else {
                    sum += item.totalDepositPrice
                }
            }

            return vPosSum.coerceAtLeast(sum)
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
    fun hasReachedMaxCheckoutLimit(): Boolean = data.hasReachedMaxCheckoutLimit

    /**
     * Returns true if the shopping cart is over the current set limit for online checkouts
     */
    fun hasReachedMaxOnlinePaymentLimit(): Boolean = data.hasReachedMaxOnlinePaymentLimit

    private fun updateTimestamp() {
        data = data.copy(
            lastModificationTime = System.currentTimeMillis()
        )
    }

    fun checkLimits() {
        val totalPrice = totalPrice
        val maxCheckoutLimit = project?.maxCheckoutLimit ?: 0
        val maxOnlinePaymentLimit = project?.maxOnlinePaymentLimit ?: 0
        if (totalPrice < maxCheckoutLimit) {
            data = data.copy(hasReachedMaxCheckoutLimit = false)
        }
        if (totalPrice < maxOnlinePaymentLimit) {
            data = data.copy(hasReachedMaxOnlinePaymentLimit = false)
        }
        if (!data.hasReachedMaxCheckoutLimit
            && maxCheckoutLimit > 0
            && totalPrice >= maxCheckoutLimit
        ) {
            data = data.copy(hasReachedMaxCheckoutLimit = true)
            notifyCheckoutLimitReached(this)
        } else if (!data.hasReachedMaxOnlinePaymentLimit
            && maxOnlinePaymentLimit > 0
            && totalPrice >= maxOnlinePaymentLimit
        ) {
            data = data.copy(hasReachedMaxOnlinePaymentLimit = true)
            notifyOnlinePaymentLimitReached(this)
        }
    }

    /**
     * Returns the current minimum age required to purchase all items of the shopping cart
     */
    val minimumAge: Int get() = data.items.maxOf { it.minimumAge }

    /**
     * Checks if the provided scanned code is contained inside the shopping cart
     */
    fun containsScannedCode(scannedCode: ScannedCode): Boolean =
        data.items.any { it.scannedCode != null && it.scannedCode?.code == scannedCode.code }

    val availablePaymentMethods: List<PaymentMethodInfo>?
        get() = updater?.lastAvailablePaymentMethods

    val isVerifiedOnline: Boolean
        get() = updater?.isUpdated == true

    fun toJson(): String = GsonHolder.get().toJson(this)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun toBackendCart(): BackendCart {
        val userPreferences = Snabble.userPreferences
        val loyaltyCardId = project?.customerCardId

        val requiredInfo = listOfNotNull(
            when (data.taxation != Taxation.UNDECIDED) {
                true -> BackendCartRequiredInformation(id = "taxation", value = data.taxation.value)
                else -> null
            }
        )
        return BackendCart(
            appUserId = userPreferences.appUser?.id,
            clientId = userPreferences.clientId,
            customer = loyaltyCardId?.let { BackendCartCustomer(it) },
            items = createBackendCartItems(),
            requiredInformation = requiredInfo,
            session = id,
            shopId = Snabble.checkedInShop?.id ?: "unknown"
        )
    }

    private fun createBackendCartItems(): MutableList<BackendCartItem> {
        val items: MutableList<BackendCartItem> = mutableListOf()
        forEach { cartItem ->
            when (cartItem?.type) {
                ItemType.PRODUCT -> {
                    val item = createBackendCartItem(cartItem)
                    items.add(item)

                    if (cartItem.coupon != null) {
                        // this item id needs to be unique per item or else the
                        // promotion engine on the backend gets confused
                        items.add(
                            BackendCartItem(
                                id = UUID.randomUUID().toString(),
                                refersTo = item.id,
                                amount = 1,
                                couponID = cartItem.coupon?.id
                            )
                        )
                    }
                }

                ItemType.COUPON -> {
                    items.add(
                        BackendCartItem(
                            id = cartItem.backendCouponId,
                            amount = 1,
                            scannedCode = cartItem.scannedCode?.code,
                            couponID = cartItem.coupon?.id
                        )
                    )
                }

                ItemType.DEPOSIT_RETURN_VOUCHER ->
                    items.add(
                        BackendCartItem(
                            id = cartItem.id,
                            amount = 1,
                            itemId = cartItem.lineItem?.itemId,
                            scannedCode = cartItem.scannedCode?.code
                        )
                    )

                else -> Unit
            }
        }
        return items
    }

    private fun createBackendCartItem(cartItem: Item): BackendCartItem {
        val product = cartItem.product
        val quantity = cartItem.getUnitBasedQuantity()
        val scannedCode = cartItem.scannedCode
        val encodingUnit = getCurrentEncodingUnit(scannedCode, product)
        return BackendCartItem(
            id = cartItem.id,
            sku = product?.sku.toString(),
            scannedCode = getSelectedScannedCode(product, scannedCode, cartItem),
            weightUnit = encodingUnit?.id,
            weight = getCurrentWeight(cartItem, product, quantity),
            amount = getCurrentAmount(cartItem, product, quantity),
            units = getCurrentUnit(cartItem),
            price = getCurrentPrice(cartItem, scannedCode)
        )
    }

    private fun getCurrentEncodingUnit(
        scannedCode: ScannedCode?,
        product: Product?
    ): Unit? = scannedCode?.embeddedUnit ?: product?.getEncodingUnit(scannedCode?.templateName, scannedCode?.lookupCode)

    private fun getCurrentUnit(cartItem: Item) = when (cartItem.unit) {
        Unit.PIECE -> cartItem.getEffectiveQuantity(true)
        else -> null
    }

    private fun getCurrentPrice(
        cartItem: Item,
        scannedCode: ScannedCode?
    ): Int? = when {
        scannedCode?.hasPrice() == true -> scannedCode.price
        cartItem.unit == Unit.PRICE -> cartItem.localTotalPrice
        else -> null
    }

    private fun getCurrentWeight(
        cartItem: Item,
        product: Product?,
        quantity: Int
    ) = when {
        cartItem.unit != Unit.PRICE && cartItem.unit != Unit.PIECE && cartItem.unit != null ->
            cartItem.getEffectiveQuantity(ignoreLineItem = true)

        product?.type == Type.UserWeighed -> quantity
        else -> null
    }

    private fun getCurrentAmount(
        cartItem: Item,
        product: Product?,
        quantity: Int
    ) = when {
        cartItem.unit == null && product?.type != Type.UserWeighed -> quantity
        else -> 1
    }

    private fun getSelectedScannedCode(
        product: Product?,
        scannedCode: ScannedCode?,
        cartItem: Item
    ): String? {
        var selectedScannedCode = product?.primaryCode?.lookupCode ?: scannedCode?.code

        // reencode user input from scanned code with 0 amount
        if (cartItem.unit == Unit.PIECE && scannedCode?.embeddedData == 0) {
            val codeTemplate = getCodeTemplateFor(scannedCode.templateName)
            val newCode = codeTemplate?.code(scannedCode.lookupCode)?.embed(cartItem.effectiveQuantity)?.buildCode()
            if (newCode != null) {
                selectedScannedCode = newCode.code
            }
        }

        return selectedScannedCode
    }

    private fun getCodeTemplateFor(templateName: String?): CodeTemplate? {
        templateName ?: return null
        return project?.getCodeTemplate(templateName)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun resolveViolations(violations: List<Violation>) {
        violations.forEach { violation ->
            data.items
                .filter { it.coupon != null }
                .filter { it.backendCouponId != null }
                .filter { it.backendCouponId == violation.refersTo }
                .reversed()
                .forEach { item ->
                    data.items.remove(item)
                    data = data.copy(modCount = modCount.inc())

                    val isViolationAlreadyPresent: Boolean =
                        data.violationNotifications.any { it.refersTo == violation.refersTo }
                    if (!isViolationAlreadyPresent) {
                        data.violationNotifications.add(
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
        updatePrices(debounce = false)
    }

    /**
     * Remove the handled ViolationNotifications.
     *
     * @param violations the handled ViolationNotifications.
     */
    fun removeViolationNotification(violations: List<ViolationNotification>) {
        data.violationNotifications.removeAll(violations)
        data = data.copy(modCount = modCount.inc())
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
        if (listeners?.contains(listener) == false) {
            listeners.add(listener)
        }
        if (data.violationNotifications.isNotEmpty()) {
            listener.onViolationDetected(data.violationNotifications)
        }
    }

    /**
     * Removes a given [ShoppingCartListener] from the list of listeners.
     *
     * @param listener the listener to remove
     */
    fun removeListener(listener: ShoppingCartListener) {
        listeners?.remove(listener)
    }

    private fun notifyItemAdded(list: ShoppingCart, item: Item) {
        updateTimestamp()
        Dispatch.mainThread {
            if (list.data.items.contains(item)) {
                listeners?.forEach { listener ->
                    listener.onItemAdded(list, item)
                }
            }
        }
    }

    private fun notifyItemRemoved(list: ShoppingCart, item: Item, pos: Int) {
        updateTimestamp()
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onItemRemoved(list, item, pos)
            }
        }
    }

    private fun notifyQuantityChanged(list: ShoppingCart?, item: Item) {
        updateTimestamp()
        Dispatch.mainThread {
            if (list?.data?.items?.contains(item) == true) {
                listeners?.forEach { listener ->
                    listener.onQuantityChanged(list, item)
                }
            }
        }
    }

    private fun notifyProductsUpdate(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onProductsUpdated(list)
            }
        }
    }

    fun notifyPriceUpdate(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onPricesUpdated(list)
            }
        }
    }

    private fun notifyTaxationChanged(list: ShoppingCart, taxation: Taxation) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onTaxationChanged(list, taxation)
            }
        }
    }

    private fun notifyCheckoutLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onCheckoutLimitReached(list)
            }
        }
    }

    private fun notifyOnlinePaymentLimitReached(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onOnlinePaymentLimitReached(list)
            }
        }
    }

    private fun notifyViolations() {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
                listener.onViolationDetected(data.violationNotifications)
            }
        }
    }

    private fun notifyCartDataChanged(list: ShoppingCart) {
        Dispatch.mainThread {
            listeners?.forEach { listener ->
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
            listeners?.forEach { listener ->
                listener.onCleared(list)
            }
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

        /**
         * Sets or Returns true  if a manual coupon (coupon applied by the user after scanning) is applied
         */
        @JvmField
        var isManualCouponApplied = false

        /**
         * Gets the user associated coupon of this item
         */
        var coupon: Coupon? = null

        // The local generated UUID of a coupon which which will be used by the backend
        var backendCouponId: String? = null

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
            if (product.type == Type.UserWeighed) {
                quantity = 0
            } else {
                product.scannableCodes.forEach { code: Product.Code? ->
                    if (code?.template != null &&
                        code.template == scannedCode.templateName &&
                        code.lookupCode != null &&
                        code.lookupCode == scannedCode.lookupCode
                    ) {
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
            if (scannedCode.hasEmbeddedData() && product.type == Type.DepositReturnVoucher) {
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

        /**
         * Associate a user applied coupon with this item. E.g. manual price reductions.
         */
        fun addManualCoupon(coupon: Coupon?) {
            if (coupon != null && coupon.type != CouponType.MANUAL) {
                throw RuntimeException("Only manual coupons can be added")
            }

            this.coupon = coupon
        }

        /**
         * Returns the effective quantity (embedded weight OR embedded price)
         * depending on the type
         */
        val effectiveQuantity: Int
            get() = getEffectiveQuantity(false)

        fun getEffectiveQuantity(ignoreLineItem: Boolean): Int {
            val scannedCode = scannedCode
            return when {
                scannedCode != null && scannedCode.hasEmbeddedData() && scannedCode.embeddedData != 0 -> scannedCode.embeddedData
                else -> getUnitBasedQuantity(ignoreLineItem)
            }
        }

        /**
         * Returns the quantity of the cart item
         */
        fun getUnitBasedQuantity(): Int = getUnitBasedQuantity(ignoreLineItem = false)

        /**
         * Returns the quantity of the cart item
         *
         * @param ignoreLineItem if set to true, only return the local quantity before backend updates
         */
        fun getUnitBasedQuantity(ignoreLineItem: Boolean): Int {
            val lineItem = lineItem
            return when {
                lineItem != null && !ignoreLineItem -> lineItem.weight ?: lineItem.units ?: lineItem.amount
                else -> quantity
            }
        }

        /**
         * Set the quantity of the cart item
         */
        fun updateQuantity(quantity: Int) {
            if (scannedCode?.hasEmbeddedData() == true && scannedCode?.embeddedData != 0) {
                return
            }

            this.quantity = quantity.coerceIn(0, MAX_QUANTITY)
            val index = cart?.data?.items?.indexOf(this) ?: -1
            if (index != -1) {
                cart?.let { currentCart ->
                    if (quantity == 0) {
                        currentCart.data.items.remove(this)
                        currentCart.notifyItemRemoved(currentCart, this, index)
                    } else {
                        currentCart.notifyQuantityChanged(cart, this)
                    }
                    currentCart.data = currentCart.data.copy(modCount = currentCart.modCount.inc())
                    currentCart.generateNewUUID()
                    currentCart.invalidateOnlinePrices()
                    currentCart.updatePrices(debounce = true)
                }
            }
        }

        /**
         * Returns the [ItemType] of the cart item
         */
        val type: ItemType?
            get() = when {
                product != null -> ItemType.PRODUCT

                coupon != null -> ItemType.COUPON

                lineItem != null -> if (lineItem?.type == LineItemType.DEPOSIT_RETURN_VOUCHER) {
                    ItemType.DEPOSIT_RETURN_VOUCHER
                } else {
                    ItemType.LINE_ITEM
                }

                else -> null
            }

        /**
         * Returns true if the item is editable by the user
         */
        val isEditable: Boolean
            get() = when {
                coupon != null && coupon?.type != CouponType.MANUAL -> false
                else -> isEditableInDialog
            }

        /**
         * Returns true if the item is editable by the user while scanning
         */
        val isEditableInDialog: Boolean
            get() = when {
                lineItem != null -> (lineItem?.type == LineItemType.DEFAULT &&
                        (scannedCode?.hasEmbeddedData() == false ||
                                scannedCode?.embeddedData == 0))

                else -> (scannedCode?.hasEmbeddedData() == false ||
                        scannedCode?.embeddedData == 0) &&
                        product?.getPrice(cart?.project?.customerCardId) != 0
            }

        /**
         * Returns true if the item can be merged with items that contain the same product and type
         */
        val isMergeable: Boolean
            get() {
                val isMergeableOverride = Snabble.isMergeable
                return isMergeableOverride?.isMergeable(this, isMergeableDefault) ?: isMergeableDefault
            }

        private val isMergeableDefault: Boolean
            get() {
                if (product == null && lineItem != null || coupon != null) return false
                return product?.type == Type.Article
                        && unit != Unit.PIECE
                        && product?.getPrice(cart?.project?.customerCardId) != 0
                        && scannedCode?.embeddedData == 0
                        && !isUsingSpecifiedQuantity
            }

        /**
         * Returns the unit associated with the cart item, or null if the cart item has no unit
         */
        val unit: Unit?
            get() = when (type) {
                ItemType.PRODUCT -> {
                    scannedCode?.embeddedUnit
                        ?: product?.getEncodingUnit(scannedCode?.templateName, scannedCode?.lookupCode)
                }

                ItemType.LINE_ITEM -> lineItem?.weightUnit?.let { Unit.fromString(it) }

                else -> null
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
            get() {
                if (type == ItemType.PRODUCT) {
                    if (unit == Unit.PRICE) {
                        scannedCode?.embeddedData?.let {
                            return it
                        }
                    }
                    return product?.getPriceForQuantity(
                        effectiveQuantity,
                        scannedCode,
                        cart?.project?.roundingMode,
                        cart?.project?.customerCardId
                    ) ?: 0
                } else {
                    return 0
                }
            }

        /**
         * Gets the total deposit of the item
         */
        val totalDepositPrice: Int
            get() {
                val lineItem = lineItem
                if (lineItem != null && lineItem.type == LineItemType.DEPOSIT) {
                    return lineItem.totalPrice
                }
                return if (product != null && product?.depositProduct != null) {
                    val price = product?.depositProduct?.getPrice(cart?.project?.customerCardId) ?: 0
                    quantity * price
                } else 0
            }

        /**
         * Returns true if the item should be displayed as a discount
         */
        val isDiscount: Boolean
            get() = lineItem != null && lineItem?.type == LineItemType.DISCOUNT

        /**
         * Gets the price after applying all price modifiers
         */
        val modifiedPrice: Int
            get() {
                val item = lineItem
                return item?.priceModifiers?.sumOf { item.amount * it.price } ?: 0
            }

        /**
         * Gets the displayed name of the product
         */
        val displayName: String?
            get() = when {
                lineItem != null -> lineItem?.name
                else -> if (type == ItemType.COUPON) {
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

                val embeddedData = scannedCode?.embeddedData ?: 0
                if (unit == Unit.PRICE || (unit == Unit.PIECE && embeddedData > 0)) {
                    val units = lineItem?.units ?: 1
                    return units.toString()
                }

                val q = effectiveQuantity
                return if (q > 0) {
                    q.toString() + if (unit != null) unit?.displayValue else ""
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
                priceText ?: return null
                return when (val quantityText = quantityText) {
                    "1" -> this.priceText
                    else -> quantityText + " " + this.priceText
                }
            }

        private val reducedPriceText: String?
            get() = when {
                lineItem?.priceModifiers?.isNotEmpty() == true -> {
                    cart?.priceFormatter?.format(totalPrice, allowZeroPrice = true)
                }

                else -> null
            }

        private val extendedPriceText: String?
            get() {
                val reducedPriceText = reducedPriceText
                return when {
                    reducedPriceText != null -> this.reducedPriceText
                    else -> {
                        val product = product ?: return null
                        val lineItem = lineItem ?: return null
                        String.format(
                            Locale.getDefault(),
                            "\u00D7 %s = %s",
                            cart?.priceFormatter?.format(product, lineItem.price),
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
                val lineItem = lineItem
                if (lineItem != null) {
                    val units = lineItem.units
                    val unitsIsAtLeastOne = units != null && units > 1

                    if (lineItem.price != 0) {
                        return if (
                            product != null
                            && unitsIsAtLeastOne
                            || (unit != Unit.PRICE
                                    && (unit != Unit.PIECE || scannedCode?.embeddedData == 0)
                                    && effectiveQuantity > 1)
                        ) {
                            extendedPriceText
                        } else {
                            if (units != null) {
                                extendedPriceText
                            } else {
                                reducedPriceText ?: cart?.priceFormatter?.format(totalPrice, true)
                            }
                        }
                    }
                }
                val product = product ?: return null
                if (product.getPrice(cart?.project?.customerCardId) > 0 || scannedCode?.hasEmbeddedData() == true) {
                    val unit = unit
                    val unitList = listOf(Unit.PRICE, Unit.PIECE)
                    return if (unit in unitList && !(scannedCode?.hasEmbeddedData() == true && scannedCode?.embeddedData == 0)) {
                        cart?.priceFormatter?.format(totalPrice)
                    } else if (effectiveQuantity <= 1) {
                        cart?.priceFormatter?.format(product)
                    } else {
                        String.format(
                            Locale.getDefault(),
                            "\u00D7 %s = %s",
                            cart?.priceFormatter?.format(product),
                            cart?.priceFormatter?.format(totalPrice)
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
                    val saleRestriction = product?.saleRestriction
                    if (saleRestriction != null && saleRestriction.isAgeRestriction) {
                        return saleRestriction.value.toInt()
                    }
                }
                return 0
            }

        val isAgeRestricted: Boolean
            get() = product?.saleRestriction?.isAgeRestriction == true

        fun replace(product: Product?, scannedCode: ScannedCode?, quantity: Int) {
            this.product = product
            this.scannedCode = scannedCode
            this.quantity = quantity
        }
    }

    companion object {

        const val MAX_QUANTITY = 99999
    }
}
