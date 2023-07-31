package io.snabble.sdk.shoppingcart

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.gson.JsonSyntaxException
import io.snabble.sdk.OnProductAvailableListener
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.checkout.CheckoutInfo
import io.snabble.sdk.checkout.CheckoutInfoResult
import io.snabble.sdk.checkout.DefaultCheckoutApi
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.checkout.PaymentMethodInfo
import io.snabble.sdk.checkout.Price
import io.snabble.sdk.checkout.SignedCheckoutInfo
import io.snabble.sdk.checkout.Violation
import io.snabble.sdk.codes.ScannedCode.Companion.parseDefault
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.CountDownLatch

internal class ShoppingCartUpdater(
    private val project: Project,
    private val cart: ShoppingCart
) {

    private val checkoutApi: DefaultCheckoutApi = DefaultCheckoutApi(project, cart)
    private val handler: Handler = Handler(Looper.getMainLooper())

    var lastAvailablePaymentMethods: List<PaymentMethodInfo>? = null
        private set
    var isUpdated = false
        private set

    private var successfulModCount = -1
    private val updatePriceRunnable = Runnable { update(false) }

    val mainScope = CoroutineScope(Dispatchers.Main + Job())

    fun update(force: Boolean) {
        Logger.d("Updating prices...")
        if (cart.isEmpty) {
            lastAvailablePaymentMethods = null
            cart.notifyPriceUpdate(cart)
            return
        }
        val modCount = cart.modCount
        if (modCount == successfulModCount && !force) {
            return
        }

        checkoutApi.createCheckoutInfo(cart.toBackendCart(), object : CheckoutInfoResult {
            override fun onSuccess(
                signedCheckoutInfo: SignedCheckoutInfo,
                onlinePrice: Int,
                availablePaymentMethods: List<PaymentMethodInfo>
            ) {

                // ignore when cart was modified mid request
                if (cart.modCount != modCount) {
                    return
                }
                val skus = getToBeReplacedSkus(signedCheckoutInfo)
                if (skus.isNotEmpty()) {
                    mainScope.launch {
                        val products = withContext(Dispatchers.Default) { getReplacedProducts(skus) }
                        if (products == null) {
                            onUnknownError()
                        } else {
                            commitCartUpdate(modCount, signedCheckoutInfo, products)
                        }
                    }
                } else {
                    commitCartUpdate(modCount, signedCheckoutInfo, null)
                }
            }

            override fun onNoShopFound() {
                error(true)
            }

            override fun onInvalidProducts(products: List<Product>) {
                cart.invalidProducts = products
                error(true)
            }

            override fun onNoAvailablePaymentMethodFound() {
                error(true)
            }

            override fun onInvalidDepositReturnVoucher() {
                cart.setInvalidDepositReturnVoucher(true)
                error(true)
            }

            override fun onUnknownError() {
                error(false)
            }

            override fun onConnectionError() {
                error(false)
            }
        }, -1)
    }

    private fun error(b: Boolean) {
        isUpdated = b
        lastAvailablePaymentMethods = null
        cart.notifyPriceUpdate(cart)
    }

    private fun commitCartUpdate(
        modCount: Int,
        signedCheckoutInfo: SignedCheckoutInfo,
        products: Map<String?, Product>?
    ) {

        if (isCardModified(modCount)) return

        cart.invalidateOnlinePrices()
        val (price, lineItems, violations) = destructorCheckoutInfo(signedCheckoutInfo) ?: return error(false)

        addViolations(violations)

        if (!cartItemMatch(lineItems)) return

        var discounts = 0

        lineItems.forEach { lineItem ->
            val item = cart.getByItemId(lineItem.id)
            if (item != null) {
                val updateIsSuccess = updateItem(item, lineItem, products)
                if (!updateIsSuccess) return
            } else {
                discounts += if (lineItem.type == LineItemType.DISCOUNT) {
                    lineItem.totalPrice
                } else {
                    insertLineItemIfNoMatchingCoupon(lineItem)
                    applyModifiedPrice(lineItem)
                }
            }
        }
        addDiscountLineItem(discounts)
        setOnlinePrice(price)
        successfulModCount = modCount
        Logger.d("Successfully updated prices")

        lastAvailablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods()
        isUpdated = true
        cart.invalidProducts = null
        cart.checkLimits()
        cart.notifyPriceUpdate(cart)
    }

    private fun destructorCheckoutInfo(
        signedCheckoutInfo: SignedCheckoutInfo
    ): Triple<Price?, List<LineItem>, List<Violation>>? {
        return try {
            val (p, l, v) = GsonHolder
                .get()
                .fromJson(
                    signedCheckoutInfo.checkoutInfo,
                    CheckoutInfo::class.java
                )
            Triple(p, l, v)
        } catch (e: JsonSyntaxException) {
            Logger.e("Could not parse Checkout info: %s", e.message)
            null
        }
    }

    private fun insertLineItemIfNoMatchingCoupon(lineItem: LineItem) {
        val hasMatchingCoupon = project.coupons.any { it.id == lineItem.couponId }

        if (!hasMatchingCoupon) {
            cart.insert(cart.newItem(lineItem), cart.size(), false)
        }
    }

    private fun applyModifiedPrice(lineItem: LineItem): Int {
        if (lineItem.type == LineItemType.COUPON) {
            val refersTo = cart.getByItemId(lineItem.refersTo)
            if (refersTo != null) {
                refersTo.isManualCouponApplied = lineItem.redeemed
                return refersTo.modifiedPrice
            }
        }
        return 0
    }

    private fun addViolations(violations: List<Violation>) {
        if (violations.isNotEmpty()) {
            cart.resolveViolations(violations)
        }
    }

    private fun addDiscountLineItem(discounts: Int) {
        if (discounts != 0) {
            val lineItem = LineItem(
                type = LineItemType.DISCOUNT,
                amount = 1,
                price = discounts,
                id = UUID.randomUUID().toString(),
                totalPrice = discounts
            )
            cart.insert(cart.newItem(lineItem), cart.size(), false)
        }
    }

    private fun setOnlinePrice(price: Price?) {
        price ?: return
        if (project.isDisplayingNetPrice) {
            cart.setOnlineTotalPrice(price.netPrice)
        } else {
            cart.setOnlineTotalPrice(price.price)
        }
    }

    private fun updateItem(
        item: ShoppingCart.Item,
        lineItem: LineItem,
        products: Map<String?, Product>?
    ): Boolean {
        if (item.product?.sku != lineItem.sku) {
            if (products == null) {
                error(false)
                return false
            }
            val product = products[lineItem.sku]
            if (product != null) {
                val scannedCode = parseDefault(project, lineItem.scannedCode)
                if (scannedCode != null) {
                    item.replace(product, scannedCode, lineItem.amount)
                    item.lineItem = lineItem
                    return true
                }
            }
        } else {
            item.lineItem = lineItem
            return true
        }
        return true
    }

    private fun cartItemMatch(lineItems: List<LineItem>): Boolean {

        val requiredIds = cart.filter { it?.type != ItemType.COUPON }.map { it?.id }
        val receivedIds = lineItems.map { it.id }

        val idsAreMatching = requiredIds.containsAll(receivedIds)

        // error out when items are missing
        if (idsAreMatching) {
            Logger.e("Missing products in price update: $requiredIds")
            error(false)
            return false
        }
        return true
    }

    private fun isCardModified(modCount: Int): Boolean {
        if (cart.modCount != modCount) {
            error(false)
            return true
        }
        return false
    }

    private fun getToBeReplacedSkus(signedCheckoutInfo: SignedCheckoutInfo): List<String?> {
        val (_, lineItems) = destructorCheckoutInfo(signedCheckoutInfo) ?: return emptyList()
        val skus: MutableList<String?> = mutableListOf()
        lineItems.forEach { currentItem ->
            val item = cart.getByItemId(itemId = currentItem.id)
            if (item?.product?.sku != currentItem.sku) {
                skus.add(currentItem.sku)
            }
        }
        return skus
    }

    private fun getReplacedProducts(skus: List<String?>): Map<String?, Product>? {
        val products: MutableMap<String?, Product> = HashMap()
        skus.forEach {
            val product = findProductBlocking(it) ?: return null
            products[it] = product
        }
        return products
    }

    private fun findProductBlocking(sku: String?): Product? {
        val countDownLatch = CountDownLatch(1)
        val products = mutableListOf<Product?>()
        project.productDatabase.findBySkuOnline(sku, object : OnProductAvailableListener {
            override fun onProductAvailable(product: Product, wasOnline: Boolean) {
                products.add(product)
                countDownLatch.countDown()
            }

            override fun onProductNotFound() {
                countDownLatch.countDown()
            }

            override fun onError() {
                countDownLatch.countDown()
            }
        })
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            Logger.e(e.message)
        }
        return products.firstOrNull()
    }

    fun dispatchUpdate() {
        handler.removeCallbacksAndMessages(this)
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + DEBOUNCE_DELAY_MS)
    }

    companion object {

        private const val DEBOUNCE_DELAY_MS = 1000
    }
}
