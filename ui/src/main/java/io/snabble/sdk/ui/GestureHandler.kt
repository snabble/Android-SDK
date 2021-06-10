package io.snabble.sdk.ui

import android.content.res.Resources
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import kotlin.math.abs

abstract class GestureHandler<T>(resources: Resources) : ItemTouchHelper.SimpleCallback(0, 0) {
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val icon =
        requireNotNull(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.snabble_ic_delete,
                null
            )
        ).let { delete ->
            delete.setTint(Color.WHITE)
            val bitmap = Bitmap.createBitmap(
                delete.intrinsicWidth,
                delete.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            delete.setBounds(0, 0, canvas.width, canvas.height)
            delete.draw(canvas)
            bitmap
        }
    private val paint = Paint().apply {
        color = 0xffd32f2f.toInt()
    }

    open fun onClick(item: T) {}
    open fun onLongClick(item: T) {}

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }

    fun startDrag(viewHolder: RecyclerView.ViewHolder): Unit = itemTouchHelper.startDrag(viewHolder)

    private val RecyclerView.isInSortMode
        get() = (adapter as? SortableAdapter)?.sortMode ?: false

    override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
            if (recyclerView.isInSortMode) ItemTouchHelper.UP + ItemTouchHelper.DOWN else 0

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
            if (recyclerView.isInSortMode) 0 else ItemTouchHelper.LEFT + ItemTouchHelper.RIGHT

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
    ): Boolean {
        val adapter = recyclerView.adapter as SortableAdapter
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        adapter.moveItem(from, to)
        return true
    }

    // based on https://stackoverflow.com/a/45565493/995926
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val itemView = viewHolder.itemView
            val height = itemView.bottom.toFloat() - itemView.top.toFloat()
            val width = height / 3f
            if (dX < 0) {
                val background = Rect(
                    itemView.right + dX.toInt(), itemView.top,
                    itemView.right, itemView.bottom
                )
                c.drawRect(background, paint)
                c.clipRect(background)

                val iconDest = RectF(
                    itemView.right - 2 * width,
                    itemView.top + width,
                    itemView.right - width,
                    itemView.bottom - width
                )
                c.drawBitmap(icon, null, iconDest, paint)
            } else if(dX > 0) {
                val background = Rect(
                    itemView.left, itemView.top,
                    itemView.left + dX.toInt(), itemView.bottom
                )
                c.drawRect(background, paint)
                c.clipRect(background)

                val iconDest = RectF(
                    itemView.left + width,
                    itemView.top + width,
                    itemView.left + width * 2,
                    itemView.bottom - width
                )
                c.drawBitmap(icon, null, iconDest, paint)
            }
        } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val alpha = MaterialColors.ALPHA_FULL - abs(dY) / viewHolder.itemView.height.toFloat()
            viewHolder.itemView.alpha = alpha
            viewHolder.itemView.translationY = dY
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val adapter = recyclerView.adapter as? DismissibleAdapter
        return if(viewHolder.bindingAdapterPosition >= 0 && adapter?.isDismissible(viewHolder.bindingAdapterPosition) == false) {
            0
        } else {
            super.getMovementFlags(recyclerView, viewHolder)
        }
    }

    interface SortableAdapter {
        val sortMode: Boolean
        fun moveItem(from: Int, to: Int)
    }

    interface DismissibleAdapter {
        fun isDismissible(position: Int): Boolean
    }
}