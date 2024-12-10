package io.snabble.sdk.checkout

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.shoppingcart.data.cart.BackendCart
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
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

    private fun ConnectivityManager.isNetworkConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCap = getNetworkCapabilities(activeNetwork) ?: return false
            listOf(
                NetworkCapabilities.TRANSPORT_WIFI,
                NetworkCapabilities.TRANSPORT_CELLULAR,
                NetworkCapabilities.TRANSPORT_ETHERNET,
                NetworkCapabilities.TRANSPORT_BLUETOOTH
            ).any(networkCap::hasTransport)
        } else {
            (@Suppress("DEPRECATION")
            activeNetworkInfo?.isConnected ?: false)
        }
    }

    fun processPendingCheckouts() {
        val context: Context = instance.application
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.isNetworkConnected()) return

        Dispatch.mainThread {
            if ((countDownLatch?.count ?: 0) > 0) {
                return@mainThread
            }

            countDownLatch = CountDownLatch(savedCarts.size)
            savedCarts.forEach { savedCart ->
                if (savedCart.failureCount >= 3) {
                    removeSavedCart(savedCart)
                }

                val checkoutApi = DefaultCheckoutApi(project, project.shoppingCart)
                checkoutApi.createCheckoutInfo(savedCart.backendCart, object : CheckoutInfoResult {
                    override fun onSuccess(
                        signedCheckoutInfo: SignedCheckoutInfo,
                        onlinePrice: Int,
                        availablePaymentMethods: List<PaymentMethodInfo>
                    ) {
                        checkoutApi.createPaymentProcess(
                            id = UUID.randomUUID().toString(),
                            signedCheckoutInfo = signedCheckoutInfo,
                            paymentMethod = fallbackPaymentMethod,
                            processedOffline = true,
                            paymentCredentials = null,
                            finalizedAt = savedCart.finalizedAt,
                            paymentProcessResult = object : PaymentProcessResult {
                                override fun onSuccess(
                                    checkoutProcessResponse: CheckoutProcessResponse?,
                                    rawResponse: String?
                                ) {
                                    Logger.d("Successfully resend checkout " + savedCart.backendCart.session)
                                    removeSavedCart(savedCart)
                                    countDownLatch?.countDown()
                                }

                                override fun onError() {
                                    fail()
                                }

                                override fun onNotFound() {
                                    fail()
                                }
                            })
                    }

                    override fun onNoShopFound() {
                        fail()
                    }

                    override fun onInvalidProducts(products: List<Product>) {
                        fail()
                    }

                    override fun onNoAvailablePaymentMethodFound() {
                        fail()
                    }

                    override fun onUnknownError() {
                        fail()
                    }

                    override fun onConnectionError() {
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
