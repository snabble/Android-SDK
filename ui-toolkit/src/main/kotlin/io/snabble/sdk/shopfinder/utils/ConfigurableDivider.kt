package io.snabble.sdk.shopfinder.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class ConfigurableDivider(
    context: Context,
    orientation: Int = VERTICAL,
    var skipLastDivider: Boolean = false,
    var justBetweenSameType: Boolean = false) : RecyclerView.ItemDecoration() {
    var drawable: Drawable
    private val bounds = Rect()

    /**
     * Sets the orientation for this divider. This should be called if
     * [RecyclerView.LayoutManager] changes orientation.
     *
     * @param orientation [.HORIZONTAL] or [.VERTICAL]
     */
    var orientation: Int = orientation
        set(value) {
            require(!(value != HORIZONTAL && orientation != VERTICAL)) { "Invalid orientation. It should be either HORIZONTAL or VERTICAL" }
            field = orientation
        }

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        drawable = a.getDrawable(0) ?: throw IllegalStateException(
            "@android:attr/listDivider was not set in the theme used for this "
                        + "DividerItemDecoration. Please set that attribute in your theme")
        a.recycle()
    }


    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null) {
            return
        }
        if (orientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }
        var childCount = parent.childCount
        val itemCount = parent.adapter?.itemCount ?: 0
        if (skipLastDivider) childCount--
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (justBetweenSameType) {
                val currentPosition = parent.getChildLayoutPosition(child)
                if (currentPosition == -1 || currentPosition >= itemCount - 1) continue
                val nextPosition = currentPosition + 1
                try {
                    if (parent.adapter?.getItemViewType(currentPosition) != parent.adapter?.getItemViewType(nextPosition)) continue
                } catch (e: IndexOutOfBoundsException) {
                    // This is a concurrent modification
                    continue
                }
            }
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val bottom = bounds.bottom + child.translationY.roundToInt()
            val top = bottom - drawable.intrinsicHeight
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft, top,
                parent.width - parent.paddingRight, bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }
        var childCount = parent.childCount
        val itemCount = parent.adapter?.itemCount ?: 0
        if (skipLastDivider) childCount--
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (justBetweenSameType) {
                val currentPosition = parent.getChildLayoutPosition(child)
                if (currentPosition == -1 || currentPosition >= itemCount - 1) continue
                val nextPosition = currentPosition + 1
                try {
                    if (parent.adapter?.getItemViewType(currentPosition) != parent.adapter?.getItemViewType(nextPosition)) continue
                } catch (e: IndexOutOfBoundsException) {
                    // This is a concurrent modification
                    continue
                }
            }
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, bounds)
            val right = bounds.right + Math.round(child.translationX)
            val left = right - drawable.intrinsicWidth
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (orientation == VERTICAL) {
            outRect[0, 0, 0] = drawable.intrinsicHeight
        } else {
            outRect[0, 0, drawable.intrinsicWidth] = 0
        }
    }

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }
}