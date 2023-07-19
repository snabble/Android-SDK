package io.snabble.sdk.ui.cart.adapter.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.adapter.models.DiscountRow

internal class DiscountViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val title: TextView = itemView.findViewById(R.id.title)
    val text: TextView = itemView.findViewById(R.id.text)

    fun update(row: DiscountRow) {
        title.text = row.title
        text.text = row.text
    }
}
