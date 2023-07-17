package io.snabble.sdk.ui.cart.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ui.R

class SimpleViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val title: TextView = itemView.findViewById(R.id.title)
    val text: TextView = itemView.findViewById(R.id.text)
    val image: ImageView = itemView.findViewById(R.id.helper_image)

    fun update(row: SnabbleSimpleRow, hasAnyImages: Boolean) {
        title.text = row.title
        text.text = row.text
        image.setImageResource(row.imageResId ?: 0)
        image.isVisible = hasAnyImages
    }
}
