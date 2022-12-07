package io.snabble.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI.Event.*
import io.snabble.sdk.ui.cart.ShoppingCartActivity
import io.snabble.sdk.ui.checkout.CheckoutActivity
import io.snabble.sdk.ui.coupon.CouponDetailActivity
import io.snabble.sdk.ui.payment.*
import io.snabble.sdk.ui.payment.payone.sepa.PayoneSepaActivity
import io.snabble.sdk.ui.scanner.SelfScanningActivity
import io.snabble.sdk.ui.search.ProductSearchActivity
import io.snabble.sdk.ui.utils.UIUtils
import java.lang.ref.WeakReference
import kotlin.collections.set

/***
 * The heart of the snabble UI components where everything connects.
 *
 * To use snabble UI components, you need to set a project you get from the core SDK using setProject.
 *
 * You can use setUiAction to implement custom behaviour or deeply integrated fragments instead
 * of the default Activites.
 */
object SnabbleUI {

    enum class Event {
        SHOW_CHECKOUT,
        SHOW_CHECKOUT_DONE,
        SHOW_SCANNER,
        SHOW_BARCODE_SEARCH,
        SHOW_SEPA_CARD_INPUT,
        SHOW_PAYONE_SEPA,
        SHOW_CREDIT_CARD_INPUT,
        SHOW_PAYONE_INPUT,
        SHOW_PAYDIREKT_INPUT,
        SHOW_SHOPPING_CART,
        SHOW_PAYMENT_CREDENTIALS_LIST,
        SHOW_PAYMENT_OPTIONS,
        SHOW_PROJECT_PAYMENT_OPTIONS,
        SHOW_AGE_VERIFICATION,
        SHOW_COUPON_DETAILS,
        GO_BACK,
        EXIT_TOKEN_AVAILABLE,
        NOT_CHECKED_IN
    }

    private class ActivityCallback(
        var activity: WeakReference<AppCompatActivity>,
        val action: Action,
    )

    private var actions = mutableMapOf<Event, ActivityCallback?>()

    @JvmStatic
    @Deprecated(
        "Use Snabble.checkedInProject instead",
        ReplaceWith("requireNotNull(Snabble.checkedInProject.value)", "io.snabble.sdk.Snabble")
    )
    val project: Project
        get() = requireNotNull(Snabble.checkedInProject.value)

    @JvmStatic
    @Deprecated(
        "Use Snabble.checkedInProject instead",
        ReplaceWith("Snabble.checkedInProject", "io.snabble.sdk.Snabble")
    )
    val projectAsLiveData: LiveData<Project?>
        get() = Snabble.checkedInProject

    /**
     * Sets an action handler for custom implementations of events.
     *
     * If no event is set, a new Activity with a default implementation will be started.
     *
     * @see Event for a list of all available events.
     */
    @JvmStatic
    fun setUiAction(activity: AppCompatActivity, event: Event, action: Action) {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            actions[event] = ActivityCallback(WeakReference<AppCompatActivity>(activity), action)

            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    actions.remove(event)
                }
            })
        }
    }

    @JvmStatic
    @JvmOverloads
    fun executeAction(context: Context, event: Event?, args: Bundle? = null) {
        val activity = UIUtils.getHostFragmentActivity(context)
        if (event == GO_BACK && activity is BaseFragmentActivity) {
            activity.finish()
            return
        }

        val cb = actions[event]
        val hostingActivity = cb?.activity?.get()
        if (cb != null) {
            if (hostingActivity != null) {
                if (hostingActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    cb.action.execute(context, args)
                } else {
                    hostingActivity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            hostingActivity.lifecycle.removeObserver(this)
                            cb.action.execute(context, args)
                        }
                    })
                }
            } else {
                actions.remove(event)
            }
        } else {
            when (event) {
                SHOW_CHECKOUT -> CheckoutActivity.startCheckoutFlow(context)

                SHOW_SCANNER ->
                    startActivity(context, SelfScanningActivity::class.java, args, canGoBack = true, unique = true)

                SHOW_BARCODE_SEARCH ->
                    startActivity(context, ProductSearchActivity::class.java, args, canGoBack = false, unique = false)

                SHOW_SEPA_CARD_INPUT ->
                    startActivity(context, SEPACardInputActivity::class.java, args, canGoBack = false)

                SHOW_PAYONE_SEPA -> startActivity(context, PayoneSepaActivity::class.java, args, canGoBack = false)

                SHOW_CREDIT_CARD_INPUT ->
                    startActivity(context, CreditCardInputActivity::class.java, args, canGoBack = false)

                SHOW_PAYONE_INPUT -> startActivity(context, PayoneInputActivity::class.java, args, canGoBack = false)

                SHOW_PAYDIREKT_INPUT ->
                    startActivity(context, PaydirektInputActivity::class.java, args, canGoBack = false)

                SHOW_SHOPPING_CART -> startActivity(context, ShoppingCartActivity::class.java, args)

                SHOW_PAYMENT_CREDENTIALS_LIST ->
                    startActivity(context, PaymentCredentialsListActivity::class.java, args)

                SHOW_PAYMENT_OPTIONS -> startActivity(context, PaymentOptionsActivity::class.java, args)

                SHOW_PROJECT_PAYMENT_OPTIONS -> startActivity(context, ProjectPaymentOptionsActivity::class.java, args)

                SHOW_AGE_VERIFICATION -> startActivity(context, AgeVerificationInputActivity::class.java, args)

                SHOW_COUPON_DETAILS -> startActivity(context, CouponDetailActivity::class.java, args)

                // unhandled actions
                GO_BACK,
                SHOW_CHECKOUT_DONE,
                NOT_CHECKED_IN,
                EXIT_TOKEN_AVAILABLE,
                null,
                -> {
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun <T> startActivity(
        context: Context,
        clazz: Class<T>, args: Bundle?,
        canGoBack: Boolean = true,
        unique: Boolean = false,
    ) {
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
