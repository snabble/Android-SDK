package io.snabble.sdk.ui.cart.shoppingcart.adapter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.SimpleRow

class LineItemViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

    fun bind(row: ProductRow) {
        composeView.setContent {
        }
    }

    fun bind(row: SimpleRow) {
        composeView.setContent {

        }
    }
}

@Composable
fun ItemDesciption(
    modifier: Modifier = Modifier,
    title: String,
    price: String
) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(text = price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
