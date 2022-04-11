package io.snabble.sdk.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.lifecycle.runtime.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Extends the `MaterialAlertDialogBuilder` by adding a lifecycle observer and attaches
 * itself to the hosting activity to detect its lifecycle events. With this you can use
 * `findViewTreeLifecycleOwner()` as expected for any `LiveData` related streams.
 */
class LifecycleAwareAlertDialogBuilder @JvmOverloads constructor(
    @NonNull context: Context,
    @StyleRes themeResId: Int = 0
): MaterialAlertDialogBuilder(context, themeResId) {
    private class DialogLifecycleOwner : LifecycleOwner, DefaultLifecycleObserver {
        private val lifecycle = LifecycleRegistry(this)

        init {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun stop() {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        /** Provide the Lifecycle of the LifecycleOwner interface */
        override fun getLifecycle(): Lifecycle = lifecycle

        // Listeners of the `DefaultLifecycleObserver` to forward the events of the owning activity.

        override fun onCreate(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onStart(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        override fun onResume(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun onPause(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun onStop(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private var activityLifecycle: Lifecycle? = null
    private val lifecycleOwner = DialogLifecycleOwner()
    private var cancelListener: DialogInterface.OnCancelListener? = null
    private var dismissListener: DialogInterface.OnDismissListener? = null

    init {
        super.setOnCancelListener {
            lifecycleOwner.stop()
            activityLifecycle?.removeObserver(lifecycleOwner)
            cancelListener?.onCancel(it)
            cancelListener = null
        }
        super.setOnDismissListener {
            lifecycleOwner.stop()
            activityLifecycle?.removeObserver(lifecycleOwner)
            dismissListener?.onDismiss(it)
            dismissListener = null
        }
    }

    override fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?) = apply {
        cancelListener = onCancelListener
    }

    override fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) = apply {
        dismissListener = onDismissListener
    }

    override fun setView(layoutResId: Int) = apply {
        setView(LayoutInflater.from(context).inflate(layoutResId, null))
    }

    override fun setView(view: View?) = apply {
        // Inject the LifecycleOwner
        view?.setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner)
        super.setView(view)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    @Deprecated("This method has been deprecated.", ReplaceWith("setView(view)"))
    override fun setView(
        view: View?,
        viewSpacingLeft: Int,
        viewSpacingTop: Int,
        viewSpacingRight: Int,
        viewSpacingBottom: Int
    ): AlertDialog.Builder {
        view?.setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner)
        return super.setView(
            view,
            viewSpacingLeft,
            viewSpacingTop,
            viewSpacingRight,
            viewSpacingBottom
        )
    }

    override fun create(): AlertDialog = super.create().also {
        activityLifecycle = (it.ownerActivity as? AppCompatActivity)?.lifecycle
        activityLifecycle?.addObserver(lifecycleOwner)
    }
}