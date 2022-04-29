package io.snabble.sdk.checkout

import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.Product
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleSdkTest
import io.snabble.sdk.codes.ScannedCode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(RobolectricTestRunner::class)
class CheckoutTest : SnabbleSdkTest() {
    private lateinit var simpleProduct1: TestProduct
    private lateinit var simpleProduct2: TestProduct
    private lateinit var simpleProduct3: TestProduct
    private lateinit var userWeighedProduct: TestProduct
    private lateinit var preWeighedProduct: TestProduct
    private lateinit var pieceProduct: TestProduct
    private lateinit var priceProduct: TestProduct
    private lateinit var zeroAmountProduct: TestProduct

    private inner class TestProduct(var product: Product, var scannedCode: ScannedCode) {
        fun cartItem(): ShoppingCart.Item {
            return cart.newItem(product, scannedCode)
        }
    }

    private fun add(testProduct: TestProduct) {
        cart.add(testProduct.cartItem())
    }

    private fun code(code: String, templateName: String): ScannedCode {
        val codes = ScannedCode.parse(project, code)
        for (scannedCode in codes) {
            if (scannedCode.templateName == templateName) {
                return scannedCode
            }
        }
        return codes[0]
    }

    @Before
    fun setup() {
        cart = project.shoppingCart

        simpleProduct1 = TestProduct(project.productDatabase.findBySku("1"), code("4008258510001", "default"))
        simpleProduct2 =
            TestProduct(project.productDatabase.findBySku("2"), code("0885580294533", "default"))
        simpleProduct3 =
            TestProduct(project.productDatabase.findBySku("3"), code("0885580466701", "default"))
        userWeighedProduct =
            TestProduct(project.productDatabase.findBySku("34"), code("23232327", "default"))
        preWeighedProduct = TestProduct(
            project.productDatabase.findBySku("34-b"),
            code("2423230001544", "ean13_instore")
        )
        pieceProduct = TestProduct(
            project.productDatabase.findBySku("34-c"),
            code("2523232000061", "ean13_instore")
        )
        priceProduct = TestProduct(
            project.productDatabase.findBySku("34-d"),
            code("2623237002494", "ean13_instore")
        )
        zeroAmountProduct = TestProduct(
            project.productDatabase.findBySku("34-c"),
            code("2523230000001", "ean13_instore")
        )
    }

    private lateinit var checkout: Checkout
    private lateinit var cart: ShoppingCart

    @Before
    fun setUp() {
        Snabble.checkedInShop = project.shops[0]
        cart = project.shoppingCart
        checkout = Checkout(project, project.shoppingCart, MockCheckoutApi(project))
    }

    @Test
    fun testCheckoutIsInNoneStateOnCreation() {
        Assert.assertEquals(Checkout.State.NONE, getOrAwaitValue(checkout.checkoutState))
    }

    @Test
    fun testCheckoutInfoIsFetched() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, getOrAwaitValue(checkout.checkoutState))
    }

    @Throws(InterruptedException::class)
    fun <T> getOrAwaitValue(liveData: LiveData<T>): T? {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer: Observer<T> = object : Observer<T> {
            override fun onChanged(@Nullable o: T) {
                data[0] = o
                latch.countDown()
                liveData.removeObserver(this)
            }
        }
        liveData.observeForever(observer)
        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw RuntimeException("LiveData value was never set.")
        }
        return data[0] as T?
    }
}
