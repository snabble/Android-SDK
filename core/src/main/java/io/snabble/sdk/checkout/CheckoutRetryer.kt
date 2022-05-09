package io.snabble.sdk.checkout

import android.content.Context
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.Project
import io.snabble.sdk.ShoppingCart.BackendCart
import android.content.SharedPreferences
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import android.net.ConnectivityManager
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.utils.Logger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

internal class CheckoutRetryer(project: Project, fallbackPaymentMethod: PaymentMethod) {
    private inner class SavedCart(
        var backendCart: BackendCart,
        var finalizedAt: Date
    ) {
        var failureCount = 0
    }

    private val fallbackPaymentMethod: PaymentMethod
    private val sharedPreferences: SharedPreferences
    private val project: Project
    private var savedCarts = CopyOnWriteArrayList<SavedCart>()
    private var countDownLatch: CountDownLatch? = null

    init {
        val context: Context = instance.application
        sharedPreferences = context.getSharedPreferences(
            "snabble_saved_checkouts_" + project.id,
            Context.MODE_PRIVATE
        )

        this.project = project
        this.fallbackPaymentMethod = fallbackPaymentMethod
        val json = sharedPreferences.getString("saved_carts", null)

        savedCarts = if (json != null) {
            val typeToken: TypeToken<*> = object : TypeToken<CopyOnWriteArrayList<SavedCart?>?>() {}
            GsonHolder.get().fromJson(json, typeToken.type)
        } else {
            CopyOnWriteArrayList()
        }

        processPendingCheckouts()
    }

    fun add(backendCart: BackendCart) {
        Dispatch.mainThread {
            savedCarts.add(SavedCart(backendCart, Date()))
            save()
        }
    }

    private fun save() {
        Dispatch.mainThread {
            val json = GsonHolder.get().toJson(savedCarts)
            sharedPreferences.edit()
                .putString("saved_carts", json)
                .apply()
        }
    }

    fun processPendingCheckouts() {
        val context: Context = instance.application
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        if (networkInfo?.isConnected == false) {
            return
        }

        Dispatch.mainThread {
            if (countDownLatch?.count ?: 0 > 0) {
                return@mainThread
            }

            countDownLatch = CountDownLatch(savedCarts.size)
            savedCarts.forEach { savedCart ->
                if (savedCart.failureCount >= 3) {
                    removeSavedCart(savedCart)
                }

                val checkoutApi = DefaultCheckoutApi(project, project.shoppingCart)
                checkoutApi.createCheckoutInfo(savedCart.backendCart, object : CheckoutInfoResult {
                    override fun success(
                        signedCheckoutInfo: SignedCheckoutInfo,
                        onlinePrice: Int,
                        availablePaymentMethods: List<PaymentMethodInfo>
                    ) {
                        checkoutApi.createPaymentProcess(
                            UUID.randomUUID().toString(),
                            signedCheckoutInfo,
                            fallbackPaymentMethod,
                            null,
                            true,
                            savedCart.finalizedAt,
                            object : PaymentProcessResult {
                                override fun success(
                                    checkoutProcessResponse: CheckoutProcessResponse?,
                                    rawResponse: String?
                                ) {
                                    Logger.d("Successfully resend checkout " + savedCart.backendCart.session)
                                    removeSavedCart(savedCart)
                                    countDownLatch?.countDown()
                                }

                                override fun error() {
                                    fail()
                                }
                            })
                    }

                    override fun noShop() {
                        fail()
                    }

                    override fun invalidProducts(products: List<Product>) {
                        fail()
                    }

                    override fun noAvailablePaymentMethod() {
                        fail()
                    }

                    override fun invalidDepositReturnVoucher() {
                        fail()
                    }

                    override fun unknownError() {
                        fail()
                    }

                    override fun connectionError() {
                        fail()
                    }

                    private fun fail() {
                        savedCart.failureCount++
                        save()
                        countDownLatch?.countDown()
                    }
                }, -1)
            }
        }
    }

    private fun removeSavedCart(savedCart: SavedCart) {
        Dispatch.mainThread {
            savedCarts.remove(savedCart)
            save()
        }
    }
}