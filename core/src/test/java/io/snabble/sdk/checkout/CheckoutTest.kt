package io.snabble.sdk.checkout

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.snabble.sdk.*
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.payment.PaymentCredentials
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutTest : SnabbleSdkTest() {
    private lateinit var simpleProduct1: TestProduct
    private lateinit var credentials: PaymentCredentials

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
    }

    private lateinit var checkout: Checkout
    private lateinit var cart: ShoppingCart
    private lateinit var mockApi: MockCheckoutApi

    @Before
    fun setUp() {
        Snabble.checkedInShop = project.shops[0]
        cart = project.shoppingCart
        mockApi = MockCheckoutApi(project)
        checkout = Checkout(project, project.shoppingCart, mockApi)
        checkout.reset()
        credentials = PaymentCredentials.fromSEPA("test", "DE11520513735120710131")
    }

    @Test
    fun testCheckoutIsInNoneStateOnCreation() {
        Assert.assertEquals(CheckoutState.NONE, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testHandleConnectionError() {
        mockApi.forceError = true
        checkout.checkout()
        Assert.assertEquals(CheckoutState.CONNECTION_ERROR, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testSuccessfulCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PROCESSING
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.PAYMENT_PROCESSING, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.SUCCESSFUL
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.PAYMENT_APPROVED, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testKeepsStateUnchanged() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())

        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testRejectedByPaymentProviderCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.FAILED
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.DENIED_BY_PAYMENT_PROVIDER, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testRejectedBySupervisorCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            checks = listOf(
                Check(
                    type = CheckType.SUPERVISOR,
                    state = CheckState.FAILED
                )
            )
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.DENIED_BY_SUPERVISOR, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testPaymentAborted() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.poll()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            aborted = true
        ))
        checkout.poll()
        Assert.assertEquals(CheckoutState.PAYMENT_ABORTED, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testFulfillments() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())
        val fulfillmentsProcessing = listOf(
            Fulfillment(
                type = "test",
                state = FulfillmentState.PROCESSING
            )
        )
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.SUCCESSFUL,
            fulfillments = fulfillmentsProcessing
        ))
        checkout.poll()
        Assert.assertEquals(fulfillmentsProcessing, checkout.fulfillmentState.getOrAwaitValue())
        val fulfillmentsProcessed = listOf(
            Fulfillment(
                type = "test",
                state = FulfillmentState.PROCESSED
            )
        )
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.SUCCESSFUL,
            fulfillments = fulfillmentsProcessed
        ))
        checkout.poll()
        Assert.assertEquals(fulfillmentsProcessed, checkout.fulfillmentState.getOrAwaitValue())
    }

    @Test
    fun testRejectWithoutShop() {
        Snabble.checkedInShop = null
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.NO_SHOP, checkout.state.getOrAwaitValue())
    }

    @Test
    fun testCheckoutKeepsStateOnPersistence() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(CheckoutState.REQUEST_PAYMENT_METHOD, checkout.state.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, checkout.state.getOrAwaitValue())

        val newCheckout = Checkout(project, project.shoppingCart, mockApi)
        Assert.assertEquals(CheckoutState.WAIT_FOR_APPROVAL, newCheckout.state.getOrAwaitValue())
    }
}
