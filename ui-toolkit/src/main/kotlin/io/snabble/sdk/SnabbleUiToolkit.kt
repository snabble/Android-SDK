package io.snabble.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.snabble.sdk.SnabbleUiToolkit.Event.*
import io.snabble.sdk.onboarding.OnboardingActivity
import io.snabble.sdk.shopfinder.ShopDetailsActivity
import io.snabble.sdk.shopfinder.ShopListActivity
import io.snabble.sdk.ui.Action
import io.snabble.sdk.ui.BaseFragmentActivity
import io.snabble.sdk.ui.utils.UIUtils.getHostFragmentActivity
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
object SnabbleUiToolkit {
    const val DEEPLINK = "deeplink"

    enum class Event {
        SHOW_ONBOARDING,
        SHOW_ONBOARDING_DONE,
        SHOW_SHOP_LIST,
        SHOW_DETAILS_SHOP_LIST,
        SHOW_DEEPLINK,
        DETAILS_SHOP_BUTTON_ACTION,
        START_NAVIGATION,
        GO_BACK
    }

    private class ActivityCallback(
        var activity: WeakReference<AppCompatActivity>,
        val action: Action
    )

    private var actions = mutableMapOf<Event, ActivityCallback?>()

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
        val activity = getHostFragmentActivity(context)
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
                SHOW_ONBOARDING -> startActivity(
                    context,
                    OnboardingActivity::class.java,
                    args,
                    false
                )
                SHOW_ONBOARDING_DONE -> activity?.finish()
                SHOW_SHOP_LIST -> startActivity(
                    context,
                    ShopListActivity::class.java,
                    args,
                    false
                )
                SHOW_DETAILS_SHOP_LIST -> startActivity(
                    context,
                    ShopDetailsActivity::class.java,
                    args,
                    true
                )
                SHOW_DEEPLINK -> {
                    val deeplink = Uri.parse(requireNotNull(args?.getString(DEEPLINK)))
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = deeplink })
                }
                // unhandled actions
                GO_BACK,
                DETAILS_SHOP_BUTTON_ACTION,
                START_NAVIGATION,
                null -> {
                }
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
