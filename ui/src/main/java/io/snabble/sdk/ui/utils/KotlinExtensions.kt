package io.snabble.sdk.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.widget.Space
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.StringNormalizer
import kotlin.math.absoluteValue
import kotlin.math.max


fun CharSequence?.isNotNullOrBlank() = !isNullOrBlank()

fun String.getImageId(context: Context): Int =
    context.resources.getIdentifier(this, "drawable", context.applicationContext.packageName)

fun String.getResourceId(context: Context):Int =
    context.resources.getIdentifier(this,"string",context.packageName)

fun String.getResourceString(context: Context): CharSequence =
    context.resources.getText(getResourceId(context))

fun String.highlight(query: String): SpannableString {
    val normalizedText = StringNormalizer.normalize(lowercase())
    val sb = SpannableString(this)
    StringNormalizer.normalize(query.lowercase())
        .split(" ")
        .filter { it.isNotEmpty() }
        .forEach { q ->
            var lastIndex = 0
            while (true) {
                lastIndex = normalizedText.indexOf(q, lastIndex)
                if (lastIndex == -1) {
                    break
                }
                val styleSpan = StyleSpan(Typeface.BOLD)
                sb.setSpan(
                    styleSpan,
                    lastIndex,
                    lastIndex + q.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
                lastIndex += q.length
            }
        }
    return sb
}

fun Snackbar.setBackgroundColor(@ColorRes color: Int) = apply {
    view.setBackgroundColor(ContextCompat.getColor(view.context, color))
}

fun Snackbar.setPriority(@UIUtils.InfoLevel level: Int) = apply {
    val textView = view.findViewById(R.id.snackbar_text) as TextView
    (view.layoutParams as? CoordinatorLayout.LayoutParams)?.gravity = Gravity.TOP
    when (level) {
        UIUtils.INFO_NEUTRAL -> {
            view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.snabble_infoColor))
            textView.setTextColor(ContextCompat.getColor(view.context, R.color.snabble_infoTextColor))
        }
        UIUtils.INFO_WARNING -> {
            view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.snabble_infoColorWarning))
            textView.setTextColor(ContextCompat.getColor(view.context, R.color.snabble_infoTextColorWarning))
        }
        UIUtils.INFO_POSITIVE -> {
            view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.snabble_infoColorPositive))
            textView.setTextColor(ContextCompat.getColor(view.context, R.color.snabble_infoTextColorPositive))
        }
    }
}

fun Snackbar.setGravity(gravity: Int) = apply {
    (view.layoutParams as? CoordinatorLayout.LayoutParams)?.gravity = gravity
    // if the view is rendered at the location y == 0 then this snackbar could be hidden behind a toolbar. Checking this and move it below.
    if (gravity == Gravity.TOP && view.y.absoluteValue < 0.0001) {
        (view.requireFragmentActivity() as AppCompatActivity).supportActionBar?.let { actionBar ->
            val y = if (actionBar.customView != null) {
                max(
                    actionBar.customView.y,
                    actionBar.customView.y + actionBar.customView.translationY + (actionBar.customView.parent as View).translationY + actionBar.height
                )
            } else {
                // attach a temp view to get the parent view (the toolbar itself)
                val orgOptions = actionBar.displayOptions
                actionBar.displayOptions = orgOptions or ActionBar.DISPLAY_SHOW_CUSTOM
                val tmp = Space(view.context)
                actionBar.customView = tmp
                val parent = tmp.parent as View
                val y = max(parent.y, parent.y + parent.translationY + (parent.parent as View).translationY + actionBar.height)
                actionBar.customView = null
                actionBar.displayOptions = orgOptions
                y
            }
            view.translationY = y
        }
    }
}