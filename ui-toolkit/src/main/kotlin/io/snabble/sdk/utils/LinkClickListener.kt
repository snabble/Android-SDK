package io.snabble.sdk.utils

import android.net.Uri
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/** Handles url click events in a spannable string */
class LinkClickListener(private val onLinkClick: (url: Uri) -> Unit) : LinkMovementMethod() {

    override fun handleMovementKey(
        widget: TextView?,
        buffer: Spannable,
        keyCode: Int,
        movementMetaState: Int,
        event: KeyEvent,
    ): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
            && KeyEvent.metaStateHasNoModifiers(movementMetaState)
            && event.action == KeyEvent.ACTION_DOWN
            && event.repeatCount == 0
        ) {
            val selectionStart = Selection.getSelectionStart(buffer)
            val selectionEnd = Selection.getSelectionEnd(buffer)

            // Selection could have been made from right to left,
            // this way we're ensuring start will be a lower number than end
            val start = min(selectionStart, selectionEnd)
            val end = max(selectionStart, selectionEnd)

            if (start != end && handleSpanClick(buffer, start, end)) return true
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event)
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val line = widget.layout.getLineForVertical(y)
            val offset = widget.layout.getOffsetForHorizontal(line, x.toFloat())
            if (handleSpanClick(buffer, offset, offset)) return true
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    private fun handleSpanClick(buffer: Spannable, start: Int, end: Int): Boolean {
        val link = buffer.getSpans(start, end, ClickableSpan::class.java)?.firstOrNull()
        if (link is URLSpan) {
            onLinkClick(Uri.parse(link.url))
            return true
        }
        return false
    }
}
