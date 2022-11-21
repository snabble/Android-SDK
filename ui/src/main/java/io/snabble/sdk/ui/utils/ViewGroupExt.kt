@file:JvmName("ViewGroupExt")
package io.snabble.sdk.ui.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(
    @LayoutRes resource: Int,
    root: ViewGroup? = this,
    attachToRoot: Boolean = false
): View = LayoutInflater.from(this.context).inflate(resource, root, attachToRoot)
