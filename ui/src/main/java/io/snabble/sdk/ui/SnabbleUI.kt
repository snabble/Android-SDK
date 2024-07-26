package io.snabble.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI.Event.EXIT_TOKEN_AVAILABLE
import io.snabble.sdk.ui.SnabbleUI.Event.GO_BACK
import io.snabble.sdk.ui.SnabbleUI.Event.NOT_CHECKED_IN
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_AGE_VERIFICATION
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_BARCODE_SEARCH
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_CHECKOUT
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_CHECKOUT_DONE
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_COUPON_DETAILS
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_EXTERNAL_BILLING
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_GIROPAY_INPUT
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PAYMENT_CREDENTIALS_LIST
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PAYMENT_OPTIONS
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PAYMENT_SELECTION_DIALOG
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PAYONE_INPUT
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PAYONE_SEPA
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_PROJECT_PAYMENT_OPTIONS
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_SCANNER
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_SEPA_CARD_INPUT
import io.snabble.sdk.ui.SnabbleUI.Event.SHOW_SHOPPING_CART
import io.snabble.sdk.ui.cart.PaymentSelectionHelper
import io.snabble.sdk.ui.cart.deprecated.ShoppingCartActivity
import io.snabble.sdk.ui.checkout.CheckoutActivity
import io.snabble.sdk.ui.coupon.CouponDetailActivity
import io.snabble.sdk.ui.payment.AgeVerificationInputActivity
import io.snabble.sdk.ui.payment.CreditCardInputActivity
import io.snabble.sdk.ui.payment.GiropayInputActivity
import io.snabble.sdk.ui.payment.PaymentCredentialsListActivity
import io.snabble.sdk.ui.payment.PaymentOptionsActivity
import io.snabble.sdk.ui.payment.PayoneInputActivity
import io.snabble.sdk.ui.payment.ProjectPaymentOptionsActivity
import io.snabble.sdk.ui.payment.SEPACardInputActivity
import io.snabble.sdk.ui.payment.externalbilling.ExternalBillingActivity
import io.snabble.sdk.ui.payment.payone.sepa.form.PayoneSepaActivity
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
        SHOW_GIROPAY_INPUT,
        SHOW_EXTERNAL_BILLING,

        @Deprecated("Will be removed in future versions. There's no replacement planned.")
        SHOW_SHOPPING_CART,
        SHOW_PAYMENT_CREDENTIALS_LIST,
        SHOW_PAYMENT_OPTIONS,
        SHOW_PROJECT_PAYMENT_OPTIONS,
        SHOW_AGE_VERIFICATION,
        SHOW_COUPON_DETAILS,
        GO_BACK,
        EXIT_TOKEN_AVAILABLE,
        NOT_CHECKED_IN,
        SHOW_PAYMENT_SELECTION_DIALOG
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

                SHOW_EXTERNAL_BILLING -> startActivity(
                    context,
                    ExternalBillingActivity::class.java,
                    args,
                    canGoBack = true
                )

                SHOW_CREDIT_CARD_INPUT ->
                    startActivity(context, CreditCardInputActivity::class.java, args, canGoBack = false)

                SHOW_PAYONE_INPUT -> startActivity(context, PayoneInputActivity::class.java, args, canGoBack = false)

                SHOW_GIROPAY_INPUT ->
                    startActivity(context, GiropayInputActivity::class.java, args, canGoBack = false)

                SHOW_SHOPPING_CART -> startActivity(context, ShoppingCartActivity::class.java, args)

                SHOW_PAYMENT_CREDENTIALS_LIST ->
                    startActivity(context, PaymentCredentialsListActivity::class.java, args)

                SHOW_PAYMENT_OPTIONS -> startActivity(context, PaymentOptionsActivity::class.java, args)

                SHOW_PROJECT_PAYMENT_OPTIONS -> startActivity(context, ProjectPaymentOptionsActivity::class.java, args)

                SHOW_AGE_VERIFICATION -> startActivity(context, AgeVerificationInputActivity::class.java, args)

                SHOW_COUPON_DETAILS -> startActivity(context, CouponDetailActivity::class.java, args)

                SHOW_PAYMENT_SELECTION_DIALOG -> PaymentSelectionHelper.getInstance()
                    .showDialog(UIUtils.getHostFragmentActivity(context))

                // unhandled actions
                GO_BACK,
                SHOW_CHECKOUT_DONE,
                NOT_CHECKED_IN,
                EXIT_TOKEN_AVAILABLE,
                null -> Unit

            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun <T> startActivity(
        context: Context,
        clazz: Class<T>, args: Bundle?,
        canGoBack: Boolean = true,
        unique: Boolean = false
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
