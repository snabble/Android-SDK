package io.snabble.sdk.checkout

import io.snabble.sdk.*
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.payment.PaymentCredentials
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    private lateinit var mockApi: MockCheckoutApi

    @Before
    fun setUp() {
        Snabble.checkedInShop = project.shops[0]
        cart = project.shoppingCart
        mockApi = MockCheckoutApi(project)
        checkout = Checkout(project, project.shoppingCart, mockApi)
        credentials = PaymentCredentials.fromSEPA("test", "DE11520513735120710131")
    }

    @Test
    fun testCheckoutIsInNoneStateOnCreation() {
        Assert.assertEquals(Checkout.State.NONE, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testSuccessfulCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, checkout.checkoutState.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PROCESSING
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.PAYMENT_PROCESSING, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.SUCCESSFUL
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.PAYMENT_APPROVED, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testKeepsStateUnchanged() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, checkout.checkoutState.getOrAwaitValue())

        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testRejectedByPaymentProviderCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, checkout.checkoutState.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.FAILED
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.DENIED_BY_PAYMENT_PROVIDER, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testRejectedBySupervisorCheckout() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, checkout.checkoutState.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            checks = listOf(
                Check(
                    type = CheckType.SUPERVISOR,
                    state = CheckState.FAILED
                )
            )
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.DENIED_BY_SUPERVISOR, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testPaymentAborted() {
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.REQUEST_PAYMENT_METHOD, checkout.checkoutState.getOrAwaitValue())
        checkout.pay(PaymentMethod.DE_DIRECT_DEBIT, credentials)
        Assert.assertEquals(Checkout.State.WAIT_FOR_APPROVAL, checkout.checkoutState.getOrAwaitValue())
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            paymentState = CheckState.PENDING
        ))
        checkout.pollForResult()
        mockApi.modifyMockResponse(CheckoutProcessResponse(
            aborted = true
        ))
        checkout.pollForResult()
        Assert.assertEquals(Checkout.State.PAYMENT_ABORTED, checkout.checkoutState.getOrAwaitValue())
    }

    @Test
    fun testRejectWithoutShop() {
        Snabble.checkedInShop = null
        add(simpleProduct1)
        checkout.checkout()
        Assert.assertEquals(Checkout.State.NO_SHOP, checkout.checkoutState.getOrAwaitValue())
    }
}
