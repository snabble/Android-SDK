package io.snabble.sdk.ui

import android.content.Context
import android.content.Intent
import io.snabble.sdk.Project
import kotlin.jvm.JvmOverloads
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI.Action.*
import io.snabble.sdk.ui.cart.ShoppingCartActivity
import io.snabble.sdk.ui.checkout.CheckoutActivity
import io.snabble.sdk.ui.payment.*
import io.snabble.sdk.ui.scanner.SelfScanningActivity
import io.snabble.sdk.ui.search.ProductSearchActivity
import io.snabble.sdk.ui.utils.UIUtils
import java.lang.ref.WeakReference

object SnabbleUI {
    enum class Action {
        SHOW_CHECKOUT,
        SHOW_CHECKOUT_DONE,
        SHOW_SCANNER,
        SHOW_BARCODE_SEARCH,
        SHOW_SEPA_CARD_INPUT,
        SHOW_CREDIT_CARD_INPUT,
        SHOW_PAYONE_INPUT,
        SHOW_PAYDIREKT_INPUT,
        SHOW_SHOPPING_CART,
        SHOW_PAYMENT_CREDENTIALS_LIST,
        SHOW_PAYMENT_OPTIONS,
        SHOW_PROJECT_PAYMENT_OPTIONS,
        SHOW_AGE_VERIFICATION,
        GO_BACK,
        EVENT_PRODUCT_CONFIRMATION_SHOW,
        EVENT_PRODUCT_CONFIRMATION_HIDE,
        EVENT_EXIT_TOKEN_AVAILABLE
    }

    interface Callback {
        fun execute(context: Context, args: Bundle?)
    }

    private class ActivityCallback(val activity: WeakReference<AppCompatActivity>,
                                   val callback: Callback)

    private val projectLiveData = MutableLiveData<Project?>()
    private var actions = mutableMapOf<Action, ActivityCallback?>()
    private var nullableProject: Project? = null

    @JvmStatic
    var project: Project
        get() {
            return requireNotNull(nullableProject)
        }
        set(value) {
            nullableProject = value
            projectLiveData.postValue(value)
        }

    @JvmStatic
    val projectAsLiveData: LiveData<Project?>
        get() = projectLiveData

    /**
     * Sets an action handler for custom implementations of screens.
     *
     * This is mostly for ease of use in Java projects. Use setUiAction in Kotlin projects.
     */
    @JvmStatic
    fun setUiCallback(activity: AppCompatActivity, action: Action, callback: Callback) {
        setUiAction(activity, action) { context, args ->
            callback.execute(context, args)
        }
    }

    /**
     * Sets an action handler for custom implementations of screens.
     */
    @JvmStatic
    fun setUiAction(activity: AppCompatActivity, action: Action, unit: ((context: Context, args: Bundle?)->Unit)?) {
        actions[action] = ActivityCallback(WeakReference<AppCompatActivity>(activity), object : Callback {
            override fun execute(context: Context, args: Bundle?) {
                unit?.invoke(context, args)
            }
        })

        activity.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                actions.remove(action)
            }
        })
    }

    @JvmStatic
    @JvmOverloads
    fun executeAction(context: Context, action: Action?, args: Bundle? = null) {
        val activity = UIUtils.getHostFragmentActivity(context)
        if (action == GO_BACK && activity is SimpleFragmentActivity) {
            activity.finish()
            return
        }

        val cb = actions[action]
        val hostingActivity = cb?.activity?.get()
        if (cb != null) {
            if (hostingActivity != null) {
                if (hostingActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    cb.callback.execute(context, args)
                } else {
                    hostingActivity.lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_START)
                        fun onStart() {
                            hostingActivity.lifecycle.removeObserver(this)
                            cb.callback.execute(context, args)
                        }
                    })

                    val intent = Intent(context, hostingActivity.javaClass)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                }
            } else {
                actions.remove(action)
            }
        } else {
            when(action) {
                SHOW_CHECKOUT -> CheckoutActivity.startCheckoutFlow(context, args)
                SHOW_SCANNER -> startActivity(context, SelfScanningActivity::class.java, args,
                    canGoBack = false,
                    unique = true
                )
                SHOW_BARCODE_SEARCH -> startActivity(context, ProductSearchActivity::class.java, args,
                    canGoBack = false,
                    unique = false
                )
                SHOW_SEPA_CARD_INPUT -> startActivity(context, SEPACardInputActivity::class.java, args, false)
                SHOW_CREDIT_CARD_INPUT -> startActivity(context, CreditCardInputActivity::class.java, args, false)
                SHOW_PAYONE_INPUT -> startActivity(context, PayoneInputActivity::class.java, args, false)
                SHOW_PAYDIREKT_INPUT -> startActivity(context, PaydirektInputActivity::class.java, args, false)
                SHOW_SHOPPING_CART -> startActivity(context, ShoppingCartActivity::class.java, args)
                SHOW_PAYMENT_CREDENTIALS_LIST -> startActivity(context, PaymentCredentialsListActivity::class.java, args)
                SHOW_PAYMENT_OPTIONS -> startActivity(context, PaymentOptionsActivity::class.java, args)
                SHOW_PROJECT_PAYMENT_OPTIONS -> startActivity(context, ProjectPaymentOptionsActivity::class.java, args)
                SHOW_AGE_VERIFICATION -> startActivity(context, AgeVerificationInputActivity::class.java, args)

                // unhandled actions
                GO_BACK,
                SHOW_CHECKOUT_DONE,
                EVENT_PRODUCT_CONFIRMATION_HIDE,
                EVENT_PRODUCT_CONFIRMATION_SHOW,
                EVENT_EXIT_TOKEN_AVAILABLE,
                null -> {}
            }
        }
    }

    internal fun <T> startActivity(context: Context,
                                  clazz: Class<T>, args: Bundle?,
                                  canGoBack: Boolean = true,
                                  unique: Boolean = false) {
        val intent = Intent(context, clazz)

        if (args != null) {
            intent.putExtras(args)
        }

        if (!canGoBack) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        if (unique) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }
}