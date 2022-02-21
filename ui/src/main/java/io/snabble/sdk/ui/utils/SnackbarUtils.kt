package io.snabble.sdk.ui.utils

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar

class SnackbarUtils {
    companion object {
        @JvmStatic
        fun make(parentView: View, string: String, duration: Int): Snackbar {
            val snackbar: Snackbar = Snackbar.make(parentView, string, duration)
            ViewCompat.setFitsSystemWindows(snackbar.view, false)
            ViewCompat.setOnApplyWindowInsetsListener(snackbar.view, null)
            return snackbar
        }

        @JvmStatic
        fun make(parentView: View, @StringRes resId: Int, duration: Int): Snackbar {
            val snackbar: Snackbar = Snackbar.make(parentView, resId, duration)
            ViewCompat.setFitsSystemWindows(snackbar.view, false)
            ViewCompat.setOnApplyWindowInsetsListener(snackbar.view, null)
            return snackbar
        }
    }
}