package io.snabble.sdk.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.startup.Initializer
import io.snabble.sdk.ui.checkout.CheckoutActivity
import io.snabble.sdk.utils.Logger

class SnabbleUIInitializerDummy

/**
 * Initializer for Snabble UI components using androidx.startup.
 */
class CheckoutContinuationInitializer : Initializer<SnabbleUIInitializerDummy> {
    override fun create(context: Context): SnabbleUIInitializerDummy {
        with(context.applicationContext as Application) {
            registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {
                    if (activity.javaClass != CheckoutActivity::class.java) {
                        CheckoutActivity.restoreCheckoutIfNeeded(context)
                    } else {
                        unregisterActivityLifecycleCallbacks(this)
                    }
                }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        }
        return SnabbleUIInitializerDummy()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}