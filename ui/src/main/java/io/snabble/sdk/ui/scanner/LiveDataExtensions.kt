@file:JvmName("LiveDataExtensions")
package io.snabble.sdk.ui.scanner

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.snabble.sdk.ui.utils.setTextOrHide

/**
 * Extension function to bind a LiveData String to the text of a TextView (or its derived
 * classes like Button)
 *
 * Might throw a IllegalArgumentException when the view is not attached.
 */
fun TextView.bindText(source: LiveData<String>) {
    val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
        "LifecycleOwner not found, please make sure that this view is already attached"
    }
    source.observe(lifecycleOwner) {
        text = it
    }
}

/**
 * Extension function to bind a LiveData String to the text of a TextView (or its derived
 * classes like Button), when the String is null or empty the view will be hidden.
 *
 * Might throw a IllegalArgumentException when the view is not attached.
 */
fun TextView.bindTextOrHide(source: LiveData<out CharSequence?>) {
    val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
        "LifecycleOwner not found, please make sure that this view is already attached"
    }
    source.observe(lifecycleOwner) {
        setTextOrHide(it)
    }
}

/**
 * Extension function to bind a LiveData String to the context description of a TextView (or
 * its derived classes like Button)
 *
 * Might throw a IllegalArgumentException when the view is not attached.
 */
fun TextView.bindContentDescription(source: LiveData<String>) {
    val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
        "LifecycleOwner not found, please make sure that this view is already attached"
    }
    source.observe(lifecycleOwner) {
        contentDescription = it
    }
}

/**
 * Extension function to bind a LiveData Boolean to the visibility of any View.
 *
 * Might throw a IllegalArgumentException when the view is not attached.
 */
fun View.bindVisibility(source: LiveData<Boolean>) {
    val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
        "LifecycleOwner not found, please make sure that this view is already attached"
    }
    source.observe(lifecycleOwner) {
        isVisible = it
    }
}

/**
 * Extension function to bind a LiveData Boolean to the enabled state of any View.
 *
 * Might throw a IllegalArgumentException when the view is not attached.
 */
fun View.bindEnabledState(source: LiveData<Boolean>) {
    val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
        "LifecycleOwner not found, please make sure that this view is already attached"
    }
    source.observe(lifecycleOwner) {
        isEnabled = it
    }
}

/**
 * Post value only when the value has changed to avoid loops, be aware that a pending update is
 * ignored.
 */
fun <T> MutableLiveData<T>.postWhenChanged(value: T) {
    if (this.value != value) postValue(value)
}