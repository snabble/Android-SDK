package io.snabble.sdk.ui.utils

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar

object SnackbarUtils {
    @JvmStatic
    fun make(parentView: View, string: String, duration: Int): Snackbar {
        val snackbar = Snackbar.make(parentView, string, duration)
        snackbar.view.fitsSystemWindows = false
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view, null)
        return snackbar
    }

    @JvmStatic
    fun make(parentView: View, @StringRes resId: Int, duration: Int): Snackbar {
        val snackbar = Snackbar.make(parentView, resId, duration)
        snackbar.view.fitsSystemWindows = false
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view, null)
        return snackbar
    }
}